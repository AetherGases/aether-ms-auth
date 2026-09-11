package com.aether.ms_auth.auth;

import com.aether.ms_auth.auth.controllers.AuthController;
import com.aether.ms_auth.auth.dto.input.ResetPasswordSendCodeInputDTO;
import com.aether.ms_auth.auth.dto.output.LoginOutputDTO;
import com.aether.ms_auth.auth.dto.request.LoginRequestDTO;
import com.aether.ms_auth.auth.dto.request.ResetPasswordChangePasswordRequestDTO;
import com.aether.ms_auth.auth.dto.request.ResetPasswordSendCodeRequestDTO;
import com.aether.ms_auth.auth.services.AuthService;
import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.services.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
public class AuthControllerTests {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private MessageService messageService;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  @Test
  @DisplayName("Should return 200 and JWT tokens on valid login")
  void loginSuccess() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("test@test.com", "Senha123");

    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000);

    LoginOutputDTO output = new LoginOutputDTO(
        "test@test.com",
        Boolean.TRUE,
        now,
        validity,
        "mocked-access-token",
        "mocked-refresh-token"
    );

    when(authService.login(any())).thenReturn(output);

    mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("mocked-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"));
  }

  @Test
  @DisplayName("Should return 400 when login body is invalid")
  void loginInvalidBody() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO("not-an-email", "");

    mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 200 and new JWT tokens on valid refresh")
  void refreshTokenSuccess() throws Exception {
    Date now = new Date();
    Date validity = new Date(now.getTime() + 3600000);

    LoginOutputDTO output = new LoginOutputDTO(
        "test@test.com",
        Boolean.TRUE,
        now,
        validity,
        "new-access-token",
        "new-refresh-token"
    );

    when(authService.refreshToken(any())).thenReturn(output);

    mockMvc.perform(put("/api/auth/refresh/{email}", "test@test.com")
            .header("Authorization", "some-refresh-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access-token"));
  }

  @Test
  @DisplayName("Should return 400 when refresh email path variable is invalid")
  void refreshTokenInvalidEmail() throws Exception {
    mockMvc.perform(put("/api/auth/refresh/{email}", "not-an-email")
            .header("Authorization", "some-refresh-token"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 204 when sending reset password code")
  void sendCodeSuccess() throws Exception {
    ResetPasswordSendCodeRequestDTO request =
        new ResetPasswordSendCodeRequestDTO("test@gmail.com");

    mockMvc.perform(post("/api/auth/reset-password/send-code")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(authService).sendCode(any(ResetPasswordSendCodeInputDTO.class));
  }

  @Test
  @DisplayName("Should return 401 when email is not found")
  void changePasswordWhenEmailIsNotFound() throws Exception {
    ResetPasswordChangePasswordRequestDTO request =
        new ResetPasswordChangePasswordRequestDTO(
            "test@gmail.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "NewPassword123"
        );

    doThrow(new UnauthorizedException("Email not found"))
        .when(authService)
        .changePassword(any());

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    verify(authService).changePassword(any());
  }

  @Test
  @DisplayName("Should return 401 when key does not exist")
  void changePasswordWhenKeyIsNotFound() throws Exception {
    ResetPasswordChangePasswordRequestDTO request =
        new ResetPasswordChangePasswordRequestDTO(
            "test@gmail.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "NewPassword123"
        );

    doThrow(new UnauthorizedException("Key not found"))
        .when(authService)
        .changePassword(any());

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    verify(authService).changePassword(any());
  }

  @Test
  @DisplayName("Should return 401 when key is incorrect")
  void changePasswordWhenKeyIsIncorrect() throws Exception {
    ResetPasswordChangePasswordRequestDTO request =
        new ResetPasswordChangePasswordRequestDTO(
            "test@gmail.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "NewPassword123"
        );

    doThrow(new UnauthorizedException("Invalid key"))
        .when(authService)
        .changePassword(any());

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    verify(authService).changePassword(any());
  }

  @Test
  @DisplayName("Should return 200 when password is changed")
  void changePasswordWhenDataIsCorrect() throws Exception {
    ResetPasswordChangePasswordRequestDTO request =
        new ResetPasswordChangePasswordRequestDTO(
            "test@gmail.com",
            "550e8400-e29b-41d4-a716-446655440000",
            "NewPassword123"
        );

    doNothing()
        .when(authService)
        .changePassword(any());

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(authService).changePassword(any());
  }
}