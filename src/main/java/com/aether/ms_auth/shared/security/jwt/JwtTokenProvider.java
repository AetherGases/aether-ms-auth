package com.aether.ms_auth.shared.security.jwt;

import com.aether.ms_auth.auth.dto.output.LoginOutputDTO;
import com.aether.ms_auth.shared.exceptions.UnauthorizedException;
import com.aether.ms_auth.shared.persistence.redis.repositories.RevokedRefreshTokensRepository;
import com.aether.ms_auth.shared.security.UserService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider {
  @Value("${spring.security.jwt.token.secret-key}")
  private String secretKey;

  @Value("${spring.security.jwt.token.expire-token-length}")
  private Integer tokenValidityInMilliseconds;
  @Value("${spring.security.jwt.token.expire-refresh-length}")
  private Integer refreshValidityInMilliseconds;

  private final UserService userService;
  private final RevokedRefreshTokensRepository revokedRefreshTokensRepository;

  Algorithm algorithm = null;

  @PostConstruct
  protected void init(){
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    algorithm = Algorithm.HMAC256(secretKey.getBytes());
  }

  public LoginOutputDTO createAccessToken(String email, List<String> permissions){
    Date now = new Date();
    Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

    String accessToken = getAccessToken(email, permissions, now, validity);
    String refreshToken = getRefreshToken(email, permissions, now);
    String role = permissions.contains("Gerenciar funcionários")
      ? "administrator"
      : "analyst";

    return new LoginOutputDTO(
        email,
        Boolean.TRUE,
        now,
        validity,
        accessToken,
        refreshToken,
        role
    );
  }

  public LoginOutputDTO refreshToken(String refreshToken, String email, List<String> permissions){
    if (!StringUtils.isEmpty(refreshToken) && refreshToken.startsWith("Bearer ")) refreshToken = refreshToken.substring("Bearer ".length());

    JWTVerifier verifier = JWT.require(algorithm).build();

    DecodedJWT decodedJWT;

    try {
      decodedJWT = verifier.verify(refreshToken);
    } catch (JWTVerificationException e) {
      throw new UnauthorizedException("exception.jwt.invalid-or-expired");
    }

    String tokenId = decodedJWT.getId();

    if (tokenId == null || !email.equals(decodedJWT.getSubject())) throw new UnauthorizedException("exception.jwt.invalid");

    Duration remainingValidity = Duration.between(Instant.now(), decodedJWT.getExpiresAtAsInstant());

    if (remainingValidity.isNegative() || remainingValidity.isZero()
        || !revokedRefreshTokensRepository.revoke(tokenId, remainingValidity)) {
      throw new UnauthorizedException("exception.jwt.invalid-or-expired");
    }

    return createAccessToken(email, permissions);
  }

  private String getRefreshToken(String email, List<String> permissions, Date now) {
    Date refreshTokenValidity = new Date(now.getTime() + refreshValidityInMilliseconds);

    return JWT.create()
        .withJWTId(UUID.randomUUID().toString())
        .withClaim("roles", permissions)
        .withIssuedAt(now)
        .withExpiresAt(refreshTokenValidity)
        .withSubject(email)
        .sign(algorithm);
  }

  private String getAccessToken(String email, List<String> permissions, Date now, Date validity) {
    String issuerUrl = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();

    return JWT.create()
        .withClaim("roles", permissions)
        .withIssuedAt(now)
        .withExpiresAt(validity)
        .withSubject(email)
        .withIssuer(issuerUrl)
        .sign(algorithm);
  }

  public Authentication getAuthentication(String token){
    DecodedJWT decodedJWT = decodedToken(token);

    UserDetails userDetails = this.userService
        .loadUserByUsername(decodedJWT.getSubject());

    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  private DecodedJWT decodedToken(String token) {
    JWTVerifier verifier = JWT.require(algorithm).build();

    try {
      return verifier.verify(token);
    } catch (JWTDecodeException | TokenExpiredException e) {
      throw new UnauthorizedException("exception.jwt.invalid-or-expired");
    }
  }

  public String resolveToken(HttpServletRequest request){
    String bearerToken = request.getHeader("Authorization");

    if (!StringUtils.isEmpty(bearerToken) && bearerToken.startsWith("Bearer ")) return bearerToken.substring("Bearer ".length());

    return null;
  }

  public boolean validateToken(String token){
    DecodedJWT decodedJWT = decodedToken(token);

    try {
      return decodedJWT.getExpiresAt().after(new Date());
    } catch (Exception e){
      throw new UnauthorizedException("exception.jwt.invalid-or-expired");
    }
  }
}
