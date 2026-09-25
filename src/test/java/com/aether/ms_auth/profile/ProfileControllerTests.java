package com.aether.ms_auth.profile;

import com.aether.ms_auth.profile.dto.request.UpdateProfileRequestDTO;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.helpers.NormalizeOutput;
import com.aether.ms_auth.shared.persistence.postgres.entities.*;
import com.aether.ms_auth.shared.persistence.postgres.repositories.*;
import com.aether.ms_auth.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_auth.shared.services.MessageService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
  private DepartmentRepository departmentRepository;

  @Autowired
  private EnterpriseRepository enterpriseRepository;

  @Autowired
  private UnitRepository unitRepository;
  @Autowired
  private PermissionGroupRepository permissionGroupRepository;

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

  private PermissionGroupEntity permissionGroup;
  private EnterpriseEntity enterprise;
  private UnitEntity unit;
  private DepartmentEntity department;
  private EmployeeEntity employeeActive;

  private EmployeeEntity employeeInactive;

  @BeforeAll
  void beforeAll() {
    enterprise = new EnterpriseEntity(
        "teste",
        "teste s.a.",
        "73414740000148"
    );

    enterpriseRepository.save(enterprise);

    unit = new UnitEntity(
        "1234567",
        "89307523000199",
        enterprise
    );

    unitRepository.save(unit);

    department = new DepartmentEntity(
        "test depto",
        "departamento de teste",
        unit
    );

    departmentRepository.save(department);

    permissionGroup = new PermissionGroupEntity(
        "teste",
        enterprise
    );

    permissionGroupRepository.save(permissionGroup);

    employeeActive = new EmployeeEntity("12345678902", "Teste", "test@test.com", passwordEncoder.encode("senha123"), "11977394517", EmployeeStatusEnum.ACTIVE, permissionGroup, department);

    employeeInactive = new EmployeeEntity("12345678903", "Teste Inativo", "test2@test.com", passwordEncoder.encode("senha123"), "11977394518", EmployeeStatusEnum.INACTIVE, permissionGroup, department);

    employeeActive = employeeRepository.save(employeeActive);
    employeeInactive = employeeRepository.save(employeeInactive);

    ids.add(employeeActive.getId());
    ids.add(employeeInactive.getId());
  }

  @AfterEach
  void afterEach() {
    employeeActive.setStatus(EmployeeStatusEnum.ACTIVE);
    employeeRepository.save(employeeActive);
  }

  @AfterAll
  void afterAll() {
    employeeRepository.deleteAllByIdInBatch(ids);
    permissionGroupRepository.delete(permissionGroup);
    departmentRepository.delete(department);
    unitRepository.delete(unit);
    enterpriseRepository.delete(enterprise);
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

  @Test
  @DisplayName("Should update profile successfully")
  void updateProfileSuccess() throws Exception {
    String token = generateJwt(employeeActive);

    String body = new ObjectMapper().writeValueAsString(
        new UpdateProfileRequestDTO("Novo Nome", "11988887777", null, null)
    );

    mockMvc.perform(
            patch("/api/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(NormalizeOutput.name("Novo Nome")))
        .andExpect(jsonPath("$.phone").value("(11) 98888-7777"));
  }

  @Test
  @DisplayName("Should update password successfully")
  void updateProfilePasswordSuccess() throws Exception {
    String token = generateJwt(employeeActive);

    String body = new ObjectMapper().writeValueAsString(
        new UpdateProfileRequestDTO(null, null, "novaSenha123", null)
    );

    mockMvc.perform(
            patch("/api/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    EmployeeEntity updated = employeeRepository.findById(employeeActive.getId()).orElseThrow();
    Assertions.assertTrue(passwordEncoder.matches("novaSenha123", updated.getPasswordHash()));
  }

  @Test
  @DisplayName("Should return 404 when employee is inactive")
  void updateProfileInactiveEmployee() throws Exception {
    String token = generateJwt(employeeActive);

    employeeActive.setStatus(EmployeeStatusEnum.INACTIVE);
    employeeRepository.save(employeeActive);

    String body = new ObjectMapper().writeValueAsString(
        new UpdateProfileRequestDTO("Novo Nome", null, null, null)
    );

    mockMvc.perform(
            patch("/api/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void updateProfileWithoutAuthentication() throws Exception {
    String body = new ObjectMapper().writeValueAsString(
        new UpdateProfileRequestDTO("Novo Nome", null, null, null)
    );

    mockMvc.perform(
            patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void updateProfileWithInvalidToken() throws Exception {
    String body = new ObjectMapper().writeValueAsString(
        new UpdateProfileRequestDTO("Novo Nome", null, null, null)
    );

    mockMvc.perform(
            patch("/api/profile")
                .header("Authorization", "Bearer token-invalido")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
  }
}