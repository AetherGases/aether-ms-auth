package com.aether.ms_auth.shared.security.jwt;

import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
  private final JwtTokenProvider tokenProvider;

  private final HandlerExceptionResolver handlerExceptionResolver;

  private static final String REFRESH_PATH_PREFIX = "/api/auth/refresh/";

  /**
   * The refresh endpoint is public and receives the *refresh* token in the Authorization header.
   * It must not be parsed here as an access token: doing so rejected the request with 401 before
   * the controller could validate the e-mail path variable (400) or the refresh token itself.
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith(REFRESH_PATH_PREFIX);
  }

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filter)
      throws IOException, ServletException {
    try{
      String token = tokenProvider.resolveToken((HttpServletRequest) request);

      if (StringUtils.isNotBlank(token) && tokenProvider.validateToken(token)){
        Authentication authentication = tokenProvider.getAuthentication(token);

        if (authentication != null){
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }

      filter.doFilter(request, response);
    } catch (UnauthorizedException e) {

      handlerExceptionResolver.resolveException(
          request,
          response,
          null,
          e
      );
    }
  }
}
