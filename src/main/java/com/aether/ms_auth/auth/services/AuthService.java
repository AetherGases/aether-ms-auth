package com.aether.ms_auth.auth.services;

import com.aether.ms_auth.auth.dto.input.*;
import com.aether.ms_auth.auth.dto.output.LoginOutputDTO;
import com.aether.ms_auth.auth.dto.output.RegisterOutputDTO;
import com.aether.ms_auth.auth.dto.output.ResetPasswordValidateCodeOutputDTO;
import com.aether.ms_auth.auth.mappers.AuthMapper;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.BadRequestException;
import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.helpers.NormalizeOutput;
import com.aether.ms_auth.shared.helpers.templates.SendCodeTemplate;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.persistence.redis.entities.GeneratedCodesDocument;
import com.aether.ms_auth.shared.persistence.redis.repositories.GeneratedCodesRepository;
import com.aether.ms_auth.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_auth.shared.services.BrevoService;
import com.aether.ms_auth.shared.services.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthenticationManager authenticationManager;

  private final JwtTokenProvider tokenProvider;

  private final EmployeeRepository employeeRepository;
  private final GeneratedCodesRepository generatedCodesRepository;

  private final PasswordEncoder passwordEncoder;
  private final BrevoService brevoService;

  private final Random random;

  @Transactional
  public LoginOutputDTO login(LoginInputDTO input){
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            input.email(),
            input.password()
        )
    );

    EmployeeEntity employee = employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new BadRequestException("exception.login.invalid")
    );

    return tokenProvider.createAccessToken(
        input.email(),
        employee.getPermissionGroups().stream().flatMap(group -> group.getPermissions().stream()).map(permission -> permission.getName()).toList()
    );
  }

  public LoginOutputDTO refreshToken(RefreshTokenInputDTO input){
    employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE).orElseThrow(
        () -> new BadRequestException("exception.login.invalid")
    );

    return tokenProvider.refreshToken(input.refreshToken());
  }

  @Transactional
  public void sendCode(ResetPasswordSendCodeInputDTO input){
    EmployeeEntity employee = employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE).orElse(null);
    GeneratedCodesDocument codeDocument = generatedCodesRepository.findByEmail(input.email());

    if (employee != null && codeDocument == null) {
      String code = String.valueOf(100000 + random.nextInt(900000));

      generatedCodesRepository.save(
          new GeneratedCodesDocument(
              employee.getId(),
              employee.getEmail(),
              passwordEncoder.encode(code)
          )
      );

      brevoService.send(new SendCodeTemplate(NormalizeOutput.name(employee.getName()), code), employee.getEmail());
    }
  }

  public ResetPasswordValidateCodeOutputDTO validateCode(ResetPasswordValidateCodeInputDTO input){
    EmployeeEntity employee = employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE).orElse(null);
    GeneratedCodesDocument codeDocument = generatedCodesRepository.findByEmail(input.email());

    if (employee != null && codeDocument != null && passwordEncoder.matches(input.code(), codeDocument.getCode())){

      String key = UUID.randomUUID().toString();

      codeDocument.setKey(passwordEncoder.encode(key));

      generatedCodesRepository.save(codeDocument);

      return new ResetPasswordValidateCodeOutputDTO(key);
    } else throw new UnauthorizedException("exception.validate-code.invalid");
  }



  @Transactional
  public void changePassword(ResetPasswordChangePasswordInputDTO input){
    EmployeeEntity employee = employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE).orElse(null);
    GeneratedCodesDocument codeDocument = generatedCodesRepository.findByEmail(input.email());

    if (employee != null && codeDocument != null && passwordEncoder.matches(input.key(), codeDocument.getKey())){
      generatedCodesRepository.deleteByEmail(input.email());

      employee.setPasswordHash(passwordEncoder.encode(input.password()));

      employeeRepository.save(employee);
    } else throw new UnauthorizedException(
          "exception.validate-key.invalid"
      );
  }

  public RegisterOutputDTO register(RegisterInputDTO input){
    EmployeeEntity employee = new EmployeeEntity(
        input.cpf(),
        input.name(),
        input.email(),
        passwordEncoder.encode(input.password()),
        input.phone()
    );

    employeeRepository.save(employee);

    return AuthMapper.convertEntityToOutput(employee);
  }
}