package com.aether.ms_auth.auth;

import com.aether.ms_auth.auth.dto.request.*;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.helpers.interfaces.BrevoTemplate;
import com.aether.ms_auth.shared.persistence.postgres.entities.*;
import com.aether.ms_auth.shared.persistence.postgres.repositories.*;
import com.aether.ms_auth.shared.persistence.redis.entities.GeneratedCodesDocument;
import com.aether.ms_auth.shared.persistence.redis.repositories.GeneratedCodesRepository;
import com.aether.ms_auth.shared.services.BrevoService;
import com.aether.ms_auth.shared.services.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the full stack (controller, service, security, Postgres and Redis) runs for real.
 * Only the outbound e-mail provider is mocked, which also lets us read the reset code that would be e-mailed.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AuthController Tests")
class AuthControllerTests {
  private static final String EMAIL = "auth-controller@test.com";
  private static final String INACTIVE_EMAIL = "auth-controller-inactive@test.com";
  private static final String PASSWORD = "Senha123";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AddressRepository addressRepository;
  @Autowired
  private EnterpriseRepository enterpriseRepository;
  @Autowired
  private UnitRepository unitRepository;
  @Autowired
  private DepartmentRepository departmentRepository;
  @Autowired
  private PermissionGroupRepository permissionGroupRepository;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private GeneratedCodesRepository generatedCodesRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private MessageService messageService;
  @MockitoBean
  private BrevoService brevoService;

  private final List<Integer> employeeIds = new ArrayList<>();
  private AddressEntity address;
  private EnterpriseEntity enterprise;
  private UnitEntity unit;
  private DepartmentEntity department;
  private PermissionGroupEntity permissionGroup;
  private EmployeeEntity employee;

  @BeforeAll
  void beforeAll() {
    address = addressRepository.save(new AddressEntity("87654321", "teste", "teste", "teste", "teste", 11, "teste"));
    enterprise = enterpriseRepository.save(new EnterpriseEntity("teste auth", "teste auth s.a.", "11222333000181", address));
    unit = unitRepository.save(new UnitEntity("7654321", "45723174000110", enterprise, address));
    department = departmentRepository.save(new DepartmentEntity("auth depto", "departamento de teste", unit));
    permissionGroup = permissionGroupRepository.save(new PermissionGroupEntity("auth teste", enterprise));

    employee = employeeRepository.save(new EmployeeEntity(
        "98765432100", "Auth Teste", EMAIL, passwordEncoder.encode(PASSWORD), "11977394519",
        EmployeeStatusEnum.ACTIVE, permissionGroup, department
    ));
    EmployeeEntity inactive = employeeRepository.save(new EmployeeEntity(
        "98765432101", "Auth Inativo", INACTIVE_EMAIL, passwordEncoder.encode(PASSWORD), "11977394520",
        EmployeeStatusEnum.INACTIVE, permissionGroup, department
    ));

    employeeIds.add(employee.getId());
    employeeIds.add(inactive.getId());
  }

  @BeforeEach
  void beforeEach() {
    generatedCodesRepository.deleteByEmail(EMAIL);
    generatedCodesRepository.deleteByEmail(INACTIVE_EMAIL);
    reset(brevoService);

    employee = employeeRepository.findById(employee.getId()).orElseThrow();
    employee.setPasswordHash(passwordEncoder.encode(PASSWORD));
    employeeRepository.save(employee);
  }

  @AfterAll
  void afterAll() {
    generatedCodesRepository.deleteByEmail(EMAIL);
    generatedCodesRepository.deleteByEmail(INACTIVE_EMAIL);
    employeeRepository.deleteAllByIdInBatch(employeeIds);
    permissionGroupRepository.delete(permissionGroup);
    departmentRepository.delete(department);
    unitRepository.delete(unit);
    enterpriseRepository.delete(enterprise);
    addressRepository.delete(address);
  }

  // ---------------------------------------------------------------- helpers

  private String json(Object body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  private JsonNode login(String email, String password) throws Exception {
    String body = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO(email, password))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    return objectMapper.readTree(body);
  }

  /** Requests a reset code and returns the plain code that was handed to the e-mail provider. */
  private String requestCode(String email) throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/send-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordSendCodeRequestDTO(email))))
        .andExpect(status().isNoContent());

    return lastSentCode(email);
  }

  private String lastSentCode(String email) {
    ArgumentCaptor<BrevoTemplate> captor = ArgumentCaptor.forClass(BrevoTemplate.class);
    verify(brevoService, atLeastOnce()).send(captor.capture(), eq(email));

    List<BrevoTemplate> sent = captor.getAllValues();
    return String.valueOf(sent.get(sent.size() - 1).params().get("code"));
  }

  private String validateCode(String email, String code) throws Exception {
    String body = mockMvc.perform(post("/api/auth/reset-password/validate-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordValidateCodeRequestDTO(email, code))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    return objectMapper.readTree(body).get("key").asText();
  }

  // ------------------------------------------------------------------ login

  @Test
  @DisplayName("Should return 200 and JWT tokens on valid login")
  void loginSuccess() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO(EMAIL, PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.role").value("analyst"));
  }

  @Test
  @DisplayName("Should return 403 when password is wrong")
  void loginWrongPassword() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO(EMAIL, "SenhaErrada1"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should return 403 when user does not exist")
  void loginUnknownUser() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO("nobody@test.com", PASSWORD))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should not log in an inactive employee")
  void loginInactiveEmployee() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO(INACTIVE_EMAIL, PASSWORD))))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.accessToken").doesNotExist());
  }

  @Test
  @DisplayName("Should return 400 when login body is invalid")
  void loginInvalidBody() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO("not-an-email", ""))))
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------- refresh

  @Test
  @DisplayName("Should return 200 and new JWT tokens on valid refresh")
  void refreshTokenSuccess() throws Exception {
    JsonNode tokens = login(EMAIL, PASSWORD);

    mockMvc.perform(put("/api/auth/refresh/{email}", EMAIL)
            .header("Authorization", "Bearer " + tokens.get("refreshToken").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.role").value("analyst"));
  }

  @Test
  @DisplayName("Should return 401 when refresh token is reused")
  void refreshTokenReuse() throws Exception {
    String refreshToken = login(EMAIL, PASSWORD).get("refreshToken").asText();

    mockMvc.perform(put("/api/auth/refresh/{email}", EMAIL)
            .header("Authorization", "Bearer " + refreshToken))
        .andExpect(status().isOk());

    mockMvc.perform(put("/api/auth/refresh/{email}", EMAIL)
            .header("Authorization", "Bearer " + refreshToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when refresh token is invalid")
  void refreshTokenInvalid() throws Exception {
    mockMvc.perform(put("/api/auth/refresh/{email}", EMAIL)
            .header("Authorization", "Bearer token-invalido"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when refresh token belongs to another user")
  void refreshTokenOtherUser() throws Exception {
    String refreshToken = login(EMAIL, PASSWORD).get("refreshToken").asText();

    EmployeeEntity other = employeeRepository.save(new EmployeeEntity(
        "98765432102", "Auth Outro", "auth-controller-other@test.com", passwordEncoder.encode(PASSWORD),
        "11977394521", EmployeeStatusEnum.ACTIVE, permissionGroup, department
    ));
    employeeIds.add(other.getId());

    mockMvc.perform(put("/api/auth/refresh/{email}", other.getEmail())
            .header("Authorization", "Bearer " + refreshToken))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 400 when refresh email is not an active employee")
  void refreshTokenUnknownEmployee() throws Exception {
    mockMvc.perform(put("/api/auth/refresh/{email}", "nobody@test.com")
            .header("Authorization", "Bearer some-refresh-token"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when refresh email path variable is invalid")
  void refreshTokenInvalidEmail() throws Exception {
    mockMvc.perform(put("/api/auth/refresh/{email}", "not-an-email")
            .header("Authorization", "Bearer some-refresh-token"))
        .andExpect(status().isBadRequest());
  }

  // -------------------------------------------------------------- send code

  @Test
  @DisplayName("Should return 204, store a hashed code and e-mail the plain code")
  void sendCodeSuccess() throws Exception {
    String code = requestCode(EMAIL);

    Assertions.assertTrue(code.matches("\\d{6}"));

    GeneratedCodesDocument stored = generatedCodesRepository.findByEmail(EMAIL);
    Assertions.assertNotNull(stored);
    Assertions.assertNotEquals(code, stored.getCode());
    Assertions.assertTrue(passwordEncoder.matches(code, stored.getCode()));
  }

  @Test
  @DisplayName("Should return 204 without sending when user does not exist")
  void sendCodeUnknownUser() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/send-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordSendCodeRequestDTO("nobody@test.com"))))
        .andExpect(status().isNoContent());

    verify(brevoService, never()).send(any(BrevoTemplate.class), anyString());
    Assertions.assertNull(generatedCodesRepository.findByEmail("nobody@test.com"));
  }

  @Test
  @DisplayName("Should return 204 without sending when user is inactive")
  void sendCodeInactiveUser() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/send-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordSendCodeRequestDTO(INACTIVE_EMAIL))))
        .andExpect(status().isNoContent());

    verify(brevoService, never()).send(any(BrevoTemplate.class), anyString());
  }

  @Test
  @DisplayName("Should not send a second code while one is pending")
  void sendCodeTwice() throws Exception {
    String firstCode = requestCode(EMAIL);

    mockMvc.perform(post("/api/auth/reset-password/send-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordSendCodeRequestDTO(EMAIL))))
        .andExpect(status().isNoContent());

    verify(brevoService, times(1)).send(any(BrevoTemplate.class), eq(EMAIL));
    Assertions.assertTrue(passwordEncoder.matches(firstCode, generatedCodesRepository.findByEmail(EMAIL).getCode()));
  }

  @Test
  @DisplayName("Should return 400 when send-code email is invalid")
  void sendCodeInvalidBody() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/send-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordSendCodeRequestDTO("not-an-email"))))
        .andExpect(status().isBadRequest());
  }

  // ------------------------------------------------------------ resend code

  @Test
  @DisplayName("Should return 204 and replace the stored code when resending")
  void resendCodeSuccess() throws Exception {
    requestCode(EMAIL);
    String oldHash = generatedCodesRepository.findByEmail(EMAIL).getCode();

    mockMvc.perform(post("/api/auth/reset-password/resend-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordResendCodeRequestDTO(EMAIL))))
        .andExpect(status().isNoContent());

    verify(brevoService, times(2)).send(any(BrevoTemplate.class), eq(EMAIL));

    String newCode = lastSentCode(EMAIL);
    String newHash = generatedCodesRepository.findByEmail(EMAIL).getCode();
    Assertions.assertNotEquals(oldHash, newHash);
    Assertions.assertTrue(passwordEncoder.matches(newCode, newHash));
  }

  @Test
  @DisplayName("Should return 204 without sending when there is no pending code")
  void resendCodeWithoutPendingCode() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/resend-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordResendCodeRequestDTO(EMAIL))))
        .andExpect(status().isNoContent());

    verify(brevoService, never()).send(any(BrevoTemplate.class), anyString());
  }

  @Test
  @DisplayName("Should return 204 without sending when user does not exist")
  void resendCodeUnknownUser() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/resend-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordResendCodeRequestDTO("nobody@test.com"))))
        .andExpect(status().isNoContent());

    verify(brevoService, never()).send(any(BrevoTemplate.class), anyString());
  }

  @Test
  @DisplayName("Should return 400 when resend-code email is invalid")
  void resendCodeInvalidBody() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/resend-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordResendCodeRequestDTO("not-an-email"))))
        .andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------- validate code

  @Test
  @DisplayName("Should return 200 and a UUID key when code is correct")
  void validateCodeSuccess() throws Exception {
    String code = requestCode(EMAIL);

    String key = validateCode(EMAIL, code);

    Assertions.assertDoesNotThrow(() -> UUID.fromString(key));
    Assertions.assertTrue(passwordEncoder.matches(key, generatedCodesRepository.findByEmail(EMAIL).getKey()));
  }

  @Test
  @DisplayName("Should return 401 when code is incorrect")
  void validateCodeIncorrect() throws Exception {
    requestCode(EMAIL);

    mockMvc.perform(post("/api/auth/reset-password/validate-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordValidateCodeRequestDTO(EMAIL, "000000"))))
        .andExpect(status().isUnauthorized());

    Assertions.assertNull(generatedCodesRepository.findByEmail(EMAIL).getKey());
  }

  @Test
  @DisplayName("Should return 401 when no code was requested")
  void validateCodeNotRequested() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/validate-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordValidateCodeRequestDTO(EMAIL, "123456"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when email is not found")
  void validateCodeUnknownEmail() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/validate-code")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordValidateCodeRequestDTO("nobody@test.com", "123456"))))
        .andExpect(status().isUnauthorized());
  }

  // -------------------------------------------------------- change password

  @Test
  @DisplayName("Should return 204, change the password and consume the code")
  void changePasswordSuccess() throws Exception {
    String key = validateCode(EMAIL, requestCode(EMAIL));

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordChangePasswordRequestDTO(EMAIL, key, "NovaSenha123"))))
        .andExpect(status().isNoContent());

    Assertions.assertNull(generatedCodesRepository.findByEmail(EMAIL));

    login(EMAIL, "NovaSenha123");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new LoginRequestDTO(EMAIL, PASSWORD))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should return 401 when key is incorrect and keep the old password")
  void changePasswordIncorrectKey() throws Exception {
    validateCode(EMAIL, requestCode(EMAIL));

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordChangePasswordRequestDTO(EMAIL, UUID.randomUUID().toString(), "NovaSenha123"))))
        .andExpect(status().isUnauthorized());

    login(EMAIL, PASSWORD);
  }

  @Test
  @DisplayName("Should return 401 when the code was not validated yet")
  void changePasswordWithoutValidatedCode() throws Exception {
    requestCode(EMAIL);

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordChangePasswordRequestDTO(EMAIL, UUID.randomUUID().toString(), "NovaSenha123"))))
        .andExpect(status().isUnauthorized());

    login(EMAIL, PASSWORD);
  }

  @Test
  @DisplayName("Should return 401 when no code was requested")
  void changePasswordNotRequested() throws Exception {
    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(new ResetPasswordChangePasswordRequestDTO(EMAIL, UUID.randomUUID().toString(), "NovaSenha123"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when the key is used a second time")
  void changePasswordKeyReuse() throws Exception {
    String key = validateCode(EMAIL, requestCode(EMAIL));
    String body = json(new ResetPasswordChangePasswordRequestDTO(EMAIL, key, "NovaSenha123"));

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/auth/reset-password/change-password")
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());
  }
}
