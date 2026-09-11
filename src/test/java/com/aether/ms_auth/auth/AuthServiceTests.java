package com.aether.ms_auth.auth;

import com.aether.ms_auth.auth.dto.input.LoginInputDTO;
import com.aether.ms_auth.auth.dto.input.RefreshTokenInputDTO;
import com.aether.ms_auth.auth.dto.input.ResetPasswordChangePasswordInputDTO;
import com.aether.ms_auth.auth.dto.input.ResetPasswordSendCodeInputDTO;
import com.aether.ms_auth.auth.dto.output.LoginOutputDTO;
import com.aether.ms_auth.auth.services.AuthService;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.exceptions.BadRequestException;
import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.helpers.interfaces.BrevoTemplate;
import com.aether.ms_auth.shared.persistence.redis.entities.GeneratedCodesDocument;
import com.aether.ms_auth.shared.persistence.redis.repositories.GeneratedCodesRepository;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_auth.shared.services.BrevoService;
import com.aether.ms_auth.shared.services.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
public class AuthServiceTests {

  @Mock
  private EmployeeRepository employeeRepository;

  @Mock
  private GeneratedCodesRepository generatedCodesRepository;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private MessageService messageService;
  @Mock
  private BrevoService brevoService;

  @Mock
  private Random random;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthService authService;

  @Test
  @DisplayName("Should return JWT tokens for a valid login")
  void loginAnUser(){
    LoginInputDTO input = new LoginInputDTO(
        "test@test.com",
        "Senha123"
    );

    EmployeeEntity expectedEntity = new EmployeeEntity(
        "12345678910",
        "teste",
        input.email(),
        "1199999999",
        EmployeeStatusEnum.ACTIVE,
        new ArrayList<>()
    );

    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000);

    LoginOutputDTO expectedOutput = new LoginOutputDTO(
        input.email(),
        Boolean.TRUE,
        now,
        validity,
        "mocked-access-token",
        "mocked-refresh-token"
    );

    when(employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE)).thenReturn(
        Optional.of(expectedEntity)
    );

    when(tokenProvider.createAccessToken(
        expectedEntity.getEmail(),
        expectedEntity.getPermissionGroups().stream().flatMap(g -> g.getPermissions().stream()).map(p -> p.getName()).toList())
    )
        .thenReturn(expectedOutput);

    LoginOutputDTO output = authService.login(input);

    assertEquals(output.email(), input.email());
    assertEquals(output.authenticated(), true);
  }

  @Test
  @DisplayName("Should throw an exception if user is not found")
  void loginThrow() {
    LoginInputDTO input = new LoginInputDTO("test@test.com", "Senha123");

    when(employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());

    when(messageService.getMessage("exception.login.invalid"))
        .thenReturn("Usuário inexistente ou senha inválida");

    assertThrows(BadRequestException.class, () -> authService.login(input));
  }

  @Test
  @DisplayName("Should return new JWT tokens for a valid refresh token")
  void refreshTokenSuccess() {
    RefreshTokenInputDTO input = new RefreshTokenInputDTO(
        "test@test.com",
        "some-refresh-token"
    );

    EmployeeEntity employee = new EmployeeEntity();
    employee.setEmail(input.email());
    employee.setStatus(EmployeeStatusEnum.ACTIVE);

    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000);

    LoginOutputDTO expectedOutput = new LoginOutputDTO(
        input.email(),
        Boolean.TRUE,
        now,
        validity,
        "mocked-access-token",
        "mocked-refresh-token"
    );

    when(employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));

    when(tokenProvider.refreshToken(input.refreshToken()))
        .thenReturn(expectedOutput);

    LoginOutputDTO result = authService.refreshToken(input);

    assertEquals(result, expectedOutput);
  }

  @Test
  @DisplayName("Should throw an exception if user is not found on refresh")
  void refreshTokenThrow() {
    RefreshTokenInputDTO input = new RefreshTokenInputDTO(
        "test@test.com",
        "some-refresh-token"
    );

    when(employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.empty());

    when(messageService.getMessage("exception.login.invalid"))
        .thenReturn("Usuário inexistente ou senha inválida");

    assertThrows(BadRequestException.class, () -> authService.refreshToken(input));
  }

  @Test
  @DisplayName("Should send if the user is founded")
  void resetPasswordSendCode () {
    ResetPasswordSendCodeInputDTO input = new ResetPasswordSendCodeInputDTO(
        "test@gmail.com"
    );

    int integerCode = 12345;
    String code = String.valueOf(100000 + integerCode);

    EmployeeEntity employee = new EmployeeEntity();
    employee.setName("Test");
    employee.setEmail(input.email());
    employee.setStatus(EmployeeStatusEnum.ACTIVE);

    when(employeeRepository.findByEmailAndStatus(input.email(), EmployeeStatusEnum.ACTIVE))
        .thenReturn(Optional.of(employee));

    when(generatedCodesRepository.findByEmail(input.email()))
        .thenReturn(null);

    when(random.nextInt(900000))
        .thenReturn(integerCode);

    doNothing()
        .when(brevoService)
        .send(any(BrevoTemplate.class), eq(employee.getEmail()));

    authService.sendCode(input);

    ArgumentCaptor<BrevoTemplate> captor =
        ArgumentCaptor.forClass(BrevoTemplate.class);

    verify(brevoService).send(
        captor.capture(),
        eq(employee.getEmail())
    );

    assertEquals(code, captor.getValue().params().get("code"));
  }

  @Test
  @DisplayName("Should not send if the user does not exist")
  void resetPasswordSendCodeWhenEmployeeNotFound() {
    ResetPasswordSendCodeInputDTO input =
        new ResetPasswordSendCodeInputDTO("test@gmail.com");

    when(employeeRepository.findByEmailAndStatus(
        input.email(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.empty());

    authService.sendCode(input);

    verify(brevoService, never()).send(
        any(BrevoTemplate.class),
        anyString()
    );
  }

  @Test
  @DisplayName("Should not send if the user already has a code")
  void resetPasswordSendCodeWhenCodeAlreadyExists() {
    ResetPasswordSendCodeInputDTO input =
        new ResetPasswordSendCodeInputDTO("test@gmail.com");

    EmployeeEntity employee = new EmployeeEntity();
    employee.setName("Test");
    employee.setEmail(input.email());
    employee.setStatus(EmployeeStatusEnum.ACTIVE);

    String code = "123456";

    GeneratedCodesDocument existingCode = new GeneratedCodesDocument(
        employee.getId(),
        employee.getEmail(),
        code
    );

    when(employeeRepository.findByEmailAndStatus(
        input.email(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.of(employee));

    when(generatedCodesRepository.findByEmail(input.email()))
        .thenReturn(existingCode);

    authService.sendCode(input);

    verify(brevoService, never()).send(
        any(BrevoTemplate.class),
        anyString()
    );
  }

  @Test
  @DisplayName("Should change password when key is correct")
  void changePasswordWhenKeyIsCorrect() {
    ResetPasswordChangePasswordInputDTO input =
        new ResetPasswordChangePasswordInputDTO(
            "test@gmail.com",
            "valid-key",
            "NewPassword123"
        );

    EmployeeEntity employee = new EmployeeEntity();
    employee.setName("Test");
    employee.setEmail(input.email());
    employee.setStatus(EmployeeStatusEnum.ACTIVE);

    GeneratedCodesDocument existingCode =
        new GeneratedCodesDocument(
            employee.getId(),
            employee.getEmail(),
            "123456"
        );

    existingCode.setKey(input.key());

    when(employeeRepository.findByEmailAndStatus(
        input.email(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.of(employee));

    when(generatedCodesRepository.findByEmail(input.email()))
        .thenReturn(existingCode);

    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

    when(passwordEncoder.encode(input.password()))
        .thenReturn("encoded-password");

    authService.changePassword(input);

    assertEquals("encoded-password", employee.getPasswordHash());

    verify(generatedCodesRepository)
        .deleteByEmail(input.email());

    verify(passwordEncoder)
        .encode(input.password());

    verify(employeeRepository)
        .save(employee);
  }

  @Test
  @DisplayName("Should throw a unauthorized exception when email is incorrect")
  void changePasswordWhenEmailIsNotFounded() {
    ResetPasswordChangePasswordInputDTO input =
        new ResetPasswordChangePasswordInputDTO(
            "test@gmail.com",
            "valid-key",
            "NewPassword123"
        );

    when(employeeRepository.findByEmailAndStatus(
        input.email(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.empty());

    when(generatedCodesRepository.findByEmail(input.email()))
        .thenReturn(null);

    assertThrows(
        UnauthorizedException.class,
        () -> authService.changePassword(input)
    );

    verify(generatedCodesRepository, never())
        .deleteByEmail(anyString());

    verify(employeeRepository, never())
        .save(any(EmployeeEntity.class));

    verify(passwordEncoder, never())
        .encode(anyString());
  }

  @Test
  @DisplayName("Should throw a unauthorized exception when code does not exists")
  void changePasswordWhenCodeIsNotFounded() {
    ResetPasswordChangePasswordInputDTO input =
        new ResetPasswordChangePasswordInputDTO(
            "test@gmail.com",
            "valid-key",
            "NewPassword123"
        );

    EmployeeEntity employee = new EmployeeEntity();
    employee.setName("Test");
    employee.setEmail(input.email());
    employee.setStatus(EmployeeStatusEnum.ACTIVE);

    when(employeeRepository.findByEmailAndStatus(
        input.email(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.of(employee));

    when(generatedCodesRepository.findByEmail(input.email()))
        .thenReturn(null);

    assertThrows(
        UnauthorizedException.class,
        () -> authService.changePassword(input)
    );

    verify(generatedCodesRepository, never())
        .deleteByEmail(anyString());

    verify(employeeRepository, never())
        .save(any(EmployeeEntity.class));

    verify(passwordEncoder, never())
        .encode(anyString());
  }

  @Test
  @DisplayName("Should throw a unauthorized exception when key is incorrect")
  void changePasswordWhenKeyIsIncorrect() {
    String key = "valid-key";
    String incorrectKey = "incorrect-key";

    ResetPasswordChangePasswordInputDTO input =
        new ResetPasswordChangePasswordInputDTO(
            "test@gmail.com",
            incorrectKey,
            "NewPassword123"
        );

    EmployeeEntity employee = new EmployeeEntity();
    employee.setName("Test");
    employee.setEmail(input.email());
    employee.setStatus(EmployeeStatusEnum.ACTIVE);

    GeneratedCodesDocument existingCode =
        new GeneratedCodesDocument(
            employee.getId(),
            employee.getEmail(),
            "123456"
        );

    existingCode.setKey(key);

    when(employeeRepository.findByEmailAndStatus(
        input.email(),
        EmployeeStatusEnum.ACTIVE
    )).thenReturn(Optional.of(employee));

    when(generatedCodesRepository.findByEmail(input.email()))
        .thenReturn(existingCode);

    assertThrows(
        UnauthorizedException.class,
        () -> authService.changePassword(input)
    );

    verify(generatedCodesRepository, never())
        .deleteByEmail(anyString());

    verify(employeeRepository, never())
        .save(any(EmployeeEntity.class));

    verify(passwordEncoder, never())
        .encode(anyString());
  }
}
