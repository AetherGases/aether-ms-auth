package com.aether.ms_auth.auth.controllers;

import com.aether.ms_auth.auth.dto.input.RefreshTokenInputDTO;
import com.aether.ms_auth.auth.dto.output.LoginOutputDTO;
import com.aether.ms_auth.auth.dto.output.RegisterOutputDTO;
import com.aether.ms_auth.auth.dto.request.LoginRequestDTO;
import com.aether.ms_auth.auth.dto.request.RegisterRequestDTO;
import com.aether.ms_auth.auth.dto.request.ResetPasswordChangePasswordRequestDTO;
import com.aether.ms_auth.auth.dto.request.ResetPasswordSendCodeRequestDTO;
import com.aether.ms_auth.auth.mappers.AuthMapper;
import com.aether.ms_auth.auth.services.AuthService;
import com.aether.ms_auth.shared.docs.AuthControllerDocs;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Rotas responsáveis pela autenticação dos usuários.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerDocs {
  private final AuthService authService;

  @Override
  @PostMapping("/login")
  public ResponseEntity<LoginOutputDTO> login(
      @RequestBody
      @Valid
      LoginRequestDTO input
  ){
    return new ResponseEntity<>(
        this.authService.login(
            AuthMapper.convertLoginRequestToInput(input)
        ),
        HttpStatus.OK
    );
  }

  @Override
  @PutMapping("/refresh/{email}")
  public ResponseEntity<LoginOutputDTO> refreshToken(
      @PathVariable
      @Valid
      @Email(message = "{validation.email.regex}")
      String email,

      @RequestHeader("Authorization")
      @NotEmpty(message = "{validation.refresh-token.required}")
      String refreshToken
  ){
    return new ResponseEntity<>(
        this.authService.refreshToken(
            new RefreshTokenInputDTO(email, refreshToken)
        ),
        HttpStatus.OK
    );
  }

  @Override
  @PostMapping("/reset-password/send-code")
  public ResponseEntity<Void> resetPasswordSendCode(
      @RequestBody
      @Valid
      ResetPasswordSendCodeRequestDTO input
  ){
    this.authService.sendCode(AuthMapper.convertSendCodeRequestToInput(input));

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reset-password/change-password")
  public ResponseEntity<Void> resetPasswordChangePassword(
      @RequestBody
      @Valid
      ResetPasswordChangePasswordRequestDTO input
  ){
    this.authService.changePassword(AuthMapper.convertChangePasswordToInput(input));

    return ResponseEntity.noContent().build();
  }

  // TO DO: remove this route after SQL's dataload is ready to execute, registering users is a first year responsability
  @Override
  @PostMapping("/register")
  public ResponseEntity<RegisterOutputDTO> register(
      @RequestBody
      @Valid
      RegisterRequestDTO input
  ){
    return new ResponseEntity<>(
        this.authService.register(
            AuthMapper.convertRegisterRequestToInput(input)
        ),
        HttpStatus.CREATED
    );
  }
}
