package com.aether.ms_auth.shared.security.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("JwtTokenFilter Tests")
class JwtTokenFilterTests {
  private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
  private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
  private final JwtTokenFilter filter = new JwtTokenFilter(tokenProvider, resolver);

  @Test
  @DisplayName("Should let the refresh route through without parsing its Authorization header as an access token")
  void refreshRouteIsNotParsed() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/auth/refresh/test@test.com");
    request.addHeader("Authorization", "Bearer not-a-jwt");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(tokenProvider, resolver);
    assertEquals(200, response.getStatus());
  }

  @Test
  @DisplayName("Should still run for protected routes")
  void protectedRouteIsFiltered() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profile");
    request.addHeader("Authorization", "Bearer some-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(tokenProvider).resolveToken(request);
    verify(chain).doFilter(request, response);
  }
}
