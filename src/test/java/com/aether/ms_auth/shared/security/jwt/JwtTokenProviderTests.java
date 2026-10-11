package com.aether.ms_auth.shared.security.jwt;

import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.security.UserService;
import com.aether.ms_auth.shared.services.MessageService;
import com.auth0.jwt.JWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTests {
  private static final String EMAIL = "employee@example.com";
  private static final String MANAGE_EMPLOYEES = "Gerenciar funcionários";

  @Mock private UserService userService;
  @Mock private MessageService messageService;
  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(userService, messageService);
    ReflectionTestUtils.setField(provider, "secretKey", "unit-test-secret-not-for-production");
    ReflectionTestUtils.setField(provider, "validityInMilliseconds", 3600000);
    provider.init();
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest())
    );
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void returnsAdministratorForEmployeeManagementPermission() {
    assertEquals("administrator",
        provider.createAccessToken(EMAIL, List.of(MANAGE_EMPLOYEES)).role());
  }

  @Test
  void returnsAnalystForOtherPermissions() {
    assertEquals("analyst",
        provider.createAccessToken(EMAIL, List.of("Visualizar inventários")).role());
  }

  @Test
  void returnsAnalystWithoutPermissions() {
    assertEquals("analyst", provider.createAccessToken(EMAIL, List.of()).role());
  }

  @Test
  void refreshUsesNewlyGrantedPermissions() {
    String refreshToken = provider.createAccessToken(
        EMAIL, List.of("Visualizar inventários")).refreshToken();
    when(userService.loadUserByUsername(EMAIL)).thenReturn(
        User.withUsername(EMAIL).password("unused")
            .authorities(MANAGE_EMPLOYEES).build());

    var output = provider.refreshToken("Bearer " + refreshToken);

    assertEquals("administrator", output.role());
    assertEquals(List.of(MANAGE_EMPLOYEES),
        JWT.decode(output.accessToken()).getClaim("roles").asList(String.class));
    assertEquals(List.of(MANAGE_EMPLOYEES),
        JWT.decode(output.refreshToken()).getClaim("roles").asList(String.class));
    verify(userService).loadUserByUsername(EMAIL);
  }

  @Test
  void refreshRemovesRevokedPermissions() {
    String refreshToken = provider.createAccessToken(
        EMAIL, List.of(MANAGE_EMPLOYEES)).refreshToken();
    when(userService.loadUserByUsername(EMAIL)).thenReturn(
        User.withUsername(EMAIL).password("unused")
            .authorities("Visualizar inventários").build());

    var output = provider.refreshToken(refreshToken);

    assertEquals("analyst", output.role());
    assertEquals(List.of("Visualizar inventários"),
        JWT.decode(output.accessToken()).getClaim("roles").asList(String.class));
    assertEquals(List.of("Visualizar inventários"),
        JWT.decode(output.refreshToken()).getClaim("roles").asList(String.class));
  }

  @Test
  void refreshRejectsInactiveOrMissingUser() {
    String refreshToken = provider.createAccessToken(EMAIL, List.of()).refreshToken();
    when(userService.loadUserByUsername(EMAIL))
        .thenThrow(new UnauthorizedException("User unavailable"));

    assertThrows(UnauthorizedException.class, () -> provider.refreshToken(refreshToken));
  }
}
