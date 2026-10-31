package com.aether.ms_auth.shared.persistence.redis.repositories;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RevokedRefreshTokensRepository {

  private static final String KEY_PREFIX = "revoked_refresh_token:";

  private final RedisTemplate<String, Object> redisTemplate;

  public RevokedRefreshTokensRepository(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }
  public boolean revoke(String tokenId, Duration ttl) {
    return Boolean.TRUE.equals(
        redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + tokenId, Boolean.TRUE, ttl)
    );
  }
}
