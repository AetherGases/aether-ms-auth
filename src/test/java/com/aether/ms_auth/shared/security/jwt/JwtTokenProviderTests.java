package com.aether.ms_auth.shared.security.jwt;

import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.persistence.redis.repositories.RevokedRefreshTokensRepository;
import com.aether.ms_auth.shared.security.UserService;
import com.auth0.jwt.JWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTests {
  private static final String EMAIL = "employee@example.com";
  private static final String MANAGE_EMPLOYEES = "Gerenciar funcionários";

  @Mock private UserService userService;
  @Mock private RevokedRefreshTokensRepository revokedRefreshTokensRepository;
  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(userService, revokedRefreshTokensRepository);
    ReflectionTestUtils.setField(provider, "secretKey", "unit-test-secret-not-for-production");
    ReflectionTestUtils.setField(provider, "tokenValidityInMilliseconds", 3600000);
    ReflectionTestUtils.setField(provider, "refreshValidityInMilliseconds", 7200000);
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
  void refreshUsesCurrentGrantedPermissions() {
    String refreshToken = provider.createAccessToken(
        EMAIL, List.of("Visualizar inventários")).refreshToken();
    when(revokedRefreshTokensRepository.revoke(anyString(), any(Duration.class))).thenReturn(true);

    var output = provider.refreshToken("Bearer " + refreshToken, EMAIL, List.of(MANAGE_EMPLOYEES));

    assertEquals("administrator", output.role());
    assertEquals(List.of(MANAGE_EMPLOYEES),
        JWT.decode(output.accessToken()).getClaim("roles").asList(String.class));
    assertEquals(List.of(MANAGE_EMPLOYEES),
        JWT.decode(output.refreshToken()).getClaim("roles").asList(String.class));
  }

  @Test
  void refreshRemovesRevokedPermissions() {
    String refreshToken = provider.createAccessToken(
        EMAIL, List.of(MANAGE_EMPLOYEES)).refreshToken();
    when(revokedRefreshTokensRepository.revoke(anyString(), any(Duration.class))).thenReturn(true);

    var output = provider.refreshToken(refreshToken, EMAIL, List.of("Visualizar inventários"));

    assertEquals("analyst", output.role());
    assertEquals(List.of("Visualizar inventários"),
        JWT.decode(output.accessToken()).getClaim("roles").asList(String.class));
    assertEquals(List.of("Visualizar inventários"),
        JWT.decode(output.refreshToken()).getClaim("roles").asList(String.class));
  }

  @Test
  void refreshCannotBeReused() {
    String refreshToken = provider.createAccessToken(EMAIL, List.of()).refreshToken();
    when(revokedRefreshTokensRepository.revoke(anyString(), any(Duration.class))).thenReturn(true, false);

    var renewed = provider.refreshToken(refreshToken, EMAIL, List.of());
    assertNotEquals(JWT.decode(refreshToken).getId(), JWT.decode(renewed.refreshToken()).getId());
    assertThrows(UnauthorizedException.class,
        () -> provider.refreshToken(refreshToken, EMAIL, List.of()));
  }

  @Test
  void refreshRejectsAnotherUser() {
    String refreshToken = provider.createAccessToken(EMAIL, List.of()).refreshToken();
    assertThrows(UnauthorizedException.class,
        () -> provider.refreshToken(refreshToken, "another@example.com", List.of()));
    verifyNoInteractions(revokedRefreshTokensRepository);
  }
}
