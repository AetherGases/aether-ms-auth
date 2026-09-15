package com.aether.ms_auth.profile;

import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.helpers.NormalizeOutput;
import com.aether.ms_auth.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_auth.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_auth.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_auth.shared.services.MessageService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("ProfileController Tests")
class ProfileControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Value("${spring.security.jwt.token.secret-key}")
  private String jwtSecret;

  @Value("${spring.security.jwt.token.expire-length}")
  private Integer jwtExpiration;

  @MockitoBean
  private MessageService messageService;

  private final List<Integer> ids = new ArrayList<>();

  private EmployeeEntity employeeActive;

  private EmployeeEntity employeeInactive;


  @BeforeEach
  void setup() {

    employeeActive = new EmployeeEntity("12345678902", "Teste", "test@test.com", passwordEncoder.encode("senha123"), "11977394517", EmployeeStatusEnum.ACTIVE, List.of());

    employeeInactive = new EmployeeEntity("12345678903", "Teste Inativo", "test2@test.com", passwordEncoder.encode("senha123"), "11977394518", EmployeeStatusEnum.INACTIVE, List.of());

    employeeActive = employeeRepository.save(employeeActive);
    employeeInactive = employeeRepository.save(employeeInactive);

    ids.add(employeeActive.getId());
    ids.add(employeeInactive.getId());
  }


  @AfterEach
  void cleanup() {
    employeeRepository.deleteAllByIdInBatch(ids);
  }

  private String generateJwt(EmployeeEntity employee) {
    return jwtTokenProvider.createAccessToken(employee.getEmail(), List.of()).accessToken();
  }

  @Test
  @DisplayName("Should return 200 and user profile")
  void getMyProfileSuccess() throws Exception {
    String token = generateJwt(employeeActive);
    mockMvc.perform(
        get("/api/profile")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.id").value(employeeActive.getId())
        )
        .andExpect(
            jsonPath("$.cpf").value(NormalizeOutput.cpf(employeeActive.getCpf()))
        ).andExpect(
            jsonPath("$.email").value(employeeActive.getEmail())
        )
        .andExpect(
            jsonPath("$.name").value(NormalizeOutput.name(employeeActive.getName()))
        )
        .andExpect(
            jsonPath("$.permissions").isEmpty()
        );
  }

  @Test
  @DisplayName("Should return 401 when user profile is not found")
  void getMyProfileNotFound() throws Exception {
    String token = generateJwt(new EmployeeEntity());
    mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 404 when employee is inactive")
  void getMyProfileInactiveEmployee() throws Exception {
    String token = generateJwt(employeeActive);

    employeeActive.setStatus(EmployeeStatusEnum.INACTIVE);
    employeeRepository.save(employeeActive);

    mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void getMyProfileWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void getMyProfileWithInvalidToken() throws Exception {
    mockMvc.perform(get("/api/profile").header("Authorization", "Bearer token-invalido")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is expired")
  void getMyProfileWithExpiredToken() throws Exception {
    String encodedSecret = Base64.getEncoder().encodeToString(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Algorithm algorithm = Algorithm.HMAC256(encodedSecret.getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    Date expiration = new Date(now.getTime() - 1000);
    String token = JWT.create().withClaim("roles", List.of()).withIssuedAt(new Date(now.getTime() - 2000)).withExpiresAt(expiration).withSubject(employeeActive.getEmail()).sign(algorithm);
    mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
  }
}