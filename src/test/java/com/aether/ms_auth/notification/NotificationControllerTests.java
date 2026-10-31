package com.aether.ms_auth.notification;

import com.aether.ms_auth.notification.controllers.NotificationController;
import com.aether.ms_auth.notification.dto.input.FindManyNotificationsInputDTO;
import com.aether.ms_auth.notification.dto.output.FindManyNotificationsOutputDTO;
import com.aether.ms_auth.notification.services.NotificationService;
import com.aether.ms_auth.shared.enums.InventoryStatusEnum;
import com.aether.ms_auth.shared.enums.InventoryTypeEnum;
import com.aether.ms_auth.shared.handlers.GlobalExceptionHandler;
import com.aether.ms_auth.shared.helpers.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for GET /api/profile/notifications. The service is mocked, so no database is required;
 * the focus is the route, the skip/take query binding (and its defaults) and the authenticated user id.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Tests")
class NotificationControllerTests {
  private static final String ROUTE = "/api/profile/notifications";
  private static final Integer USER_ID = 11;

  @Mock
  private NotificationService notificationService;

  private MockMvc mockMvc;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages/messages");
    messageSource.setDefaultEncoding("UTF-8");

    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.setValidationMessageSource(messageSource);
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
        .setControllerAdvice(new GlobalExceptionHandler(null))
        .setValidator(validator)
        .build();

    CustomUserDetails user = new CustomUserDetails("user@test.com", "x", List.of(), USER_ID);
    authentication = new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());

    lenient().when(notificationService.findManyNotifications(any())).thenAnswer(invocation -> {
      FindManyNotificationsInputDTO input = invocation.getArgument(0);
      return new FindManyNotificationsOutputDTO(List.of(), 0L, input.skip(), input.take());
    });
  }

  private FindManyNotificationsInputDTO capturedInput() {
    ArgumentCaptor<FindManyNotificationsInputDTO> captor = ArgumentCaptor.forClass(FindManyNotificationsInputDTO.class);
    verify(notificationService).findManyNotifications(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("Should default skip to 0 and take to 20")
  void defaults() throws Exception {
    mockMvc.perform(get(ROUTE).principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.skip").value(0))
        .andExpect(jsonPath("$.take").value(20));

    FindManyNotificationsInputDTO input = capturedInput();
    assertEquals(0, input.skip());
    assertEquals(20, input.take());
  }

  @Test
  @DisplayName("Should bind skip and take from the query string")
  void customParams() throws Exception {
    mockMvc.perform(get(ROUTE).param("skip", "40").param("take", "5").principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.skip").value(40))
        .andExpect(jsonPath("$.take").value(5));

    FindManyNotificationsInputDTO input = capturedInput();
    assertEquals(40, input.skip());
    assertEquals(5, input.take());
  }

  @Test
  @DisplayName("Should default only the param that was omitted")
  void partialParams() throws Exception {
    mockMvc.perform(get(ROUTE).param("take", "50").principal(authentication))
        .andExpect(status().isOk());

    FindManyNotificationsInputDTO input = capturedInput();
    assertEquals(0, input.skip());
    assertEquals(50, input.take());
  }

  @Test
  @DisplayName("Should use the id of the authenticated user")
  void usesAuthenticatedUser() throws Exception {
    mockMvc.perform(get(ROUTE).principal(authentication)).andExpect(status().isOk());

    assertEquals(USER_ID, capturedInput().userId());
  }

  @Test
  @DisplayName("Should serialize notifications returned by the service")
  void serializesNotifications() throws Exception {
    doReturn(
        new FindManyNotificationsOutputDTO(
            List.of(new FindManyNotificationsOutputDTO.Notification(
                "n1", "titulo", "descricao",
                new FindManyNotificationsOutputDTO.Inventory(9, InventoryStatusEnum.ISSUED, InventoryTypeEnum.OUTPUT),
                Instant.parse("2026-01-01T10:00:00Z")
            )),
            1L, 0, 20
        )
    ).when(notificationService).findManyNotifications(any());

    mockMvc.perform(get(ROUTE).principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.notifications[0].id").value("n1"))
        .andExpect(jsonPath("$.notifications[0].title").value("titulo"))
        .andExpect(jsonPath("$.notifications[0].inventory.id").value(9))
        .andExpect(jsonPath("$.notifications[0].inventory.status").value("ISSUED"))
        .andExpect(jsonPath("$.notifications[0].inventory.type").value("OUTPUT"));
  }

  @Test
  @DisplayName("Should answer 400 with a message when take is greater than 100")
  void takeAboveMax() throws Exception {
    mockMvc.perform(get(ROUTE).param("take", "101").principal(authentication))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("O take deve ser no máximo 100."));

    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("Should accept take equal to 100")
  void takeAtMax() throws Exception {
    mockMvc.perform(get(ROUTE).param("take", "100").principal(authentication))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should answer 400 with a message when take is lower than 1")
  void takeBelowMin() throws Exception {
    for (String take : new String[]{"0", "-1"}) {
      mockMvc.perform(get(ROUTE).param("take", take).principal(authentication))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("O take deve ser no mínimo 1."));
    }

    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("Should answer 400 with a message when skip is negative")
  void skipBelowMin() throws Exception {
    mockMvc.perform(get(ROUTE).param("skip", "-1").principal(authentication))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("O skip deve ser no mínimo 0."));

    verifyNoInteractions(notificationService);
  }

  @Test
  @DisplayName("Should answer 400 when skip is not a number")
  void invalidSkip() throws Exception {
    mockMvc.perform(get(ROUTE).param("skip", "abc").principal(authentication))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should answer 400 when take is not a number")
  void invalidTake() throws Exception {
    mockMvc.perform(get(ROUTE).param("take", "abc").principal(authentication))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should not expose the old /api/notifications route")
  void oldRouteIsGone() throws Exception {
    mockMvc.perform(get("/api/notifications").principal(authentication))
        .andExpect(status().isNotFound());
  }
}
