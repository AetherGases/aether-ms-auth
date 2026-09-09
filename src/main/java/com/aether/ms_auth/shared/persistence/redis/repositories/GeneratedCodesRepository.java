package com.aether.ms_auth.shared.persistence.redis.repositories;

import com.aether.ms_auth.shared.persistence.redis.entities.GeneratedCodesDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;

@Repository
public class GeneratedCodesRepository {

  private static final String KEY_PREFIX = "generated_code:";
  private static final Duration TTL = Duration.ofMinutes(30);

  private final RedisTemplate<String, Object> redisTemplate;
  private final HashOperations<String, String, Object> hashOperations;
  private final ObjectMapper objectMapper;

  public GeneratedCodesRepository(
      RedisTemplate<String, Object> redisTemplate,
      ObjectMapper objectMapper
  ) {
    this.redisTemplate = redisTemplate;
    this.hashOperations = redisTemplate.opsForHash();
    this.objectMapper = objectMapper;
  }

  public GeneratedCodesDocument save(GeneratedCodesDocument document) {
    String key = KEY_PREFIX + document.getEmail();

    Map<String, Object> fields = objectMapper.convertValue(
        document, new TypeReference<Map<String, Object>>() {}
    );

    hashOperations.putAll(key, fields);
    redisTemplate.expire(key, TTL);

    return document;
  }

  public GeneratedCodesDocument findByEmail(String email) {
    Map<String, Object> data = hashOperations.entries(KEY_PREFIX + email);

    if (data.isEmpty()) {
      return null;
    }

    return objectMapper.convertValue(data, GeneratedCodesDocument.class);
  }

  public void deleteByEmail(String email) {
    redisTemplate.delete(KEY_PREFIX + email);
  }
}