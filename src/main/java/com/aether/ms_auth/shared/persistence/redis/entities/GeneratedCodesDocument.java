package com.aether.ms_auth.shared.persistence.redis.entities;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("generated_codes")
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class GeneratedCodesDocument {
  @Id
  private String email;
  @Field(name = "user_id")
  private Integer userId;
  private String code;

  private String key;

  public GeneratedCodesDocument(Integer userId, String email, String code) {
    this.userId = userId;
    this.email = email;
    this.code = code;
  }
}