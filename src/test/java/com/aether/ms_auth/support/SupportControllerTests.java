package com.aether.ms_auth.support;

import com.aether.ms_auth.shared.AetherConstants;
import com.aether.ms_auth.shared.enums.EmployeeStatusEnum;
import com.aether.ms_auth.shared.enums.ProblemCategoryEnum;
import com.aether.ms_auth.shared.persistence.postgres.entities.*;
import com.aether.ms_auth.shared.persistence.postgres.repositories.*;
import com.aether.ms_auth.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_auth.shared.services.MessageService;
import com.aether.ms_auth.support.dto.request.RegisterSupportMessageRequestDTO;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SupportController Tests")
public class SupportControllerTests {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private StorageFileRepository storageFileRepository;
  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private AddressRepository addressRepository;

  @Autowired
  private DepartmentRepository departmentRepository;

  @Autowired
  private EnterpriseRepository enterpriseRepository;

  @Autowired
  private UnitRepository unitRepository;
  @Autowired
  private PermissionGroupRepository permissionGroupRepository;

  @Autowired
  private ReportedProblemRepository reportedProblemRepository;

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
  private final List<Integer> problemIds = new ArrayList<>();
  private final List<Integer> imageIds = new ArrayList<>();
  private StorageFileEntity storageFile;
  private PermissionGroupEntity permissionGroup;
  private EnterpriseEntity enterprise;
  private UnitEntity unit;
  private DepartmentEntity department;
  private AddressEntity address;
  private EmployeeEntity employeeActive;

  private EmployeeEntity employeeInactive;

  @BeforeAll
  void beforeAll() {
    address = new AddressEntity(
        "12345678",
        "teste",
        "teste",
        "teste",
        "teste",
        11,
        "teste"
    );
    addressRepository.save(address);

    storageFile = new StorageFileEntity(
        "foto.png",
        "https://cloudinary/teste"
    );

    storageFileRepository.save(storageFile);

    enterprise = new EnterpriseEntity(
        "teste",
        "teste s.a.",
        "73414740000148",
        address
    );

    enterpriseRepository.save(enterprise);

    unit = new UnitEntity(
        "1234567",
        "89307523000199",
        enterprise,
        address
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

    employeeActive = new EmployeeEntity("12345678902", "Teste", "test@test.com", passwordEncoder.encode("senha123"), "11977394517", EmployeeStatusEnum.ACTIVE, permissionGroup, department, storageFile);

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
    reportedProblemRepository.deleteAllByIdInBatch(problemIds);
    storageFileRepository.deleteAllByIdInBatch(imageIds);
    employeeRepository.deleteAllByIdInBatch(ids);
    permissionGroupRepository.delete(permissionGroup);
    departmentRepository.delete(department);
    unitRepository.delete(unit);
    enterpriseRepository.delete(enterprise);
    storageFileRepository.delete(storageFile);
  }

  private String generateJwt(EmployeeEntity employee) {
    return jwtTokenProvider.createAccessToken(employee.getEmail(), List.of()).accessToken();
  }

  private static final String SCREEN = "Histórico de Relatórios";
  private static final String DESCRIPTION = "Enquanto estava enviando um novo relatório apareceu uma mensagem de erro: \"500 - Internal Server Error\", como posso resolvê-lo?";

  private String body(ProblemCategoryEnum category, String screen, String description) throws Exception {
    return new ObjectMapper().writeValueAsString(
        new RegisterSupportMessageRequestDTO(category, screen, description, null)
    );
  }

  private ResultActions postSupport(String token, String body) throws Exception {
    return mockMvc.perform(
        post("/api/support")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
    );
  }

  @Test
  @DisplayName("Should return 401 when JWT is not provided")
  void reportProblemWhenJwtIsNotProvided() throws Exception {
    long before = reportedProblemRepository.count();

    mockMvc.perform(
            post("/api/support")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION)))
        .andExpect(status().isUnauthorized());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 401 when JWT is invalid")
  void reportProblemWithInvalidToken() throws Exception {
    postSupport("token-invalido", body(ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 200 and persist the problem in the database")
  void reportProblemSuccess() throws Exception {
    String token = generateJwt(employeeActive);

    MvcResult result = postSupport(token, body(ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.category").value("BUG"))
        .andExpect(jsonPath("$.screen").value(SCREEN))
        .andExpect(jsonPath("$.description").value(DESCRIPTION))
        .andReturn();

    int reportedProblemId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    problemIds.add(reportedProblemId);

    ReportedProblemEntity persisted = reportedProblemRepository.findById(reportedProblemId).orElse(null);

    assertNotNull(persisted, "the reported problem must be stored in the database");
    assertEquals(ProblemCategoryEnum.BUG, persisted.getCategory());
    assertEquals(SCREEN, persisted.getScreen());
    assertEquals(DESCRIPTION, persisted.getDescription());
  }

  @Test
  @DisplayName("Should persist the problem together with its image")
  void reportProblemWithImage() throws Exception {
    String json = new ObjectMapper().writeValueAsString(
        new RegisterSupportMessageRequestDTO(
            ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION,
            new RegisterSupportMessageRequestDTO.ImageRequest("print.png", "https://cloudinary/print.png")
        )
    );

    MvcResult result = postSupport(generateJwt(employeeActive), json)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.image.id").isNumber())
        .andExpect(jsonPath("$.image.name").value("print.png"))
        .andExpect(jsonPath("$.image.path").value("https://cloudinary/print.png"))
        .andReturn();

    int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    problemIds.add(id);

    ReportedProblemEntity persisted = reportedProblemRepository.findById(id).orElseThrow();
    assertNotNull(persisted.getStorageFile());
    assertEquals("print.png", persisted.getStorageFile().getName());
    imageIds.add(persisted.getStorageFile().getId());
  }

  @Test
  @DisplayName("Should return 400 and not persist when the image url is not https")
  void reportProblemWithInvalidImageUrl() throws Exception {
    long before = reportedProblemRepository.count();
    String json = new ObjectMapper().writeValueAsString(
        new RegisterSupportMessageRequestDTO(
            ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION,
            new RegisterSupportMessageRequestDTO.ImageRequest("print.png", "ftp://x/print.png")
        )
    );

    postSupport(generateJwt(employeeActive), json).andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should persist one new row per request")
  void reportProblemCreatesOneRowPerRequest() throws Exception {
    String token = generateJwt(employeeActive);
    long before = reportedProblemRepository.count();

    for (ProblemCategoryEnum category : List.of(ProblemCategoryEnum.PERFORMANCE, ProblemCategoryEnum.PRIVACY)) {
      MvcResult result = postSupport(token, body(category, SCREEN, DESCRIPTION))
          .andExpect(status().isOk())
          .andReturn();

      problemIds.add(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    assertEquals(before + 2, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 400 and not persist when category is missing")
  void reportProblemWithoutCategory() throws Exception {
    long before = reportedProblemRepository.count();

    postSupport(generateJwt(employeeActive), body(null, SCREEN, DESCRIPTION))
        .andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 400 and not persist when category is not a valid value")
  void reportProblemWithInvalidCategory() throws Exception {
    long before = reportedProblemRepository.count();
    String json = "{\"category\":\"NOT_A_CATEGORY\",\"screen\":\"" + SCREEN + "\",\"description\":\"" + "descrição válida" + "\"}";

    postSupport(generateJwt(employeeActive), json).andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 400 and not persist when screen is blank")
  void reportProblemWithBlankScreen() throws Exception {
    long before = reportedProblemRepository.count();

    postSupport(generateJwt(employeeActive), body(ProblemCategoryEnum.BUG, "   ", DESCRIPTION))
        .andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 400 and not persist when screen exceeds the max length")
  void reportProblemWithTooLongScreen() throws Exception {
    long before = reportedProblemRepository.count();
    String screen = "a".repeat(AetherConstants.MAX_REPORT_PROBLEM_SCREEN_LENGTH + 1);

    postSupport(generateJwt(employeeActive), body(ProblemCategoryEnum.BUG, screen, DESCRIPTION))
        .andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 400 and not persist when description is blank")
  void reportProblemWithBlankDescription() throws Exception {
    long before = reportedProblemRepository.count();

    postSupport(generateJwt(employeeActive), body(ProblemCategoryEnum.BUG, SCREEN, ""))
        .andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 400 and not persist when description exceeds the max length")
  void reportProblemWithTooLongDescription() throws Exception {
    long before = reportedProblemRepository.count();
    String description = "a".repeat(AetherConstants.MAX_REPORT_PROBLEM_DESCRIPTION_LENGTH + 1);

    postSupport(generateJwt(employeeActive), body(ProblemCategoryEnum.BUG, SCREEN, description))
        .andExpect(status().isBadRequest());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should accept screen and description at the max length")
  void reportProblemAtMaxLengths() throws Exception {
    String screen = "a".repeat(AetherConstants.MAX_REPORT_PROBLEM_SCREEN_LENGTH);
    String description = "a".repeat(AetherConstants.MAX_REPORT_PROBLEM_DESCRIPTION_LENGTH);

    MvcResult result = postSupport(generateJwt(employeeActive), body(ProblemCategoryEnum.OTHERS, screen, description))
        .andExpect(status().isOk())
        .andReturn();

    int id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    problemIds.add(id);

    ReportedProblemEntity persisted = reportedProblemRepository.findById(id).orElseThrow();
    assertEquals(screen, persisted.getScreen());
    assertEquals(description, persisted.getDescription());
  }

  @Test
  @DisplayName("Should return 400 when the body is missing")
  void reportProblemWithoutBody() throws Exception {
    mockMvc.perform(
            post("/api/support")
                .header("Authorization", "Bearer " + generateJwt(employeeActive))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 401 when employee is inactive")
  void reportProblemInactiveEmployee() throws Exception {
    String token = generateJwt(employeeActive);
    employeeActive.setStatus(EmployeeStatusEnum.INACTIVE);
    employeeRepository.save(employeeActive);

    long before = reportedProblemRepository.count();

    postSupport(token, body(ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION))
        .andExpect(status().isUnauthorized());

    assertEquals(before, reportedProblemRepository.count());
  }

  @Test
  @DisplayName("Should return 401 when JWT is expired")
  void reportProblemWithExpiredToken() throws Exception {
    String encodedSecret = Base64.getEncoder().encodeToString(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Algorithm algorithm = Algorithm.HMAC256(encodedSecret.getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    String token = JWT.create()
        .withClaim("roles", List.of())
        .withIssuedAt(new Date(now.getTime() - 2000))
        .withExpiresAt(new Date(now.getTime() - 1000))
        .withSubject(employeeActive.getEmail())
        .sign(algorithm);

    long before = reportedProblemRepository.count();

    postSupport(token, body(ProblemCategoryEnum.BUG, SCREEN, DESCRIPTION))
        .andExpect(status().isUnauthorized());

    assertEquals(before, reportedProblemRepository.count());
  }
}
