package com.aether.ms_auth.shared.persistence.mongo.documents;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "device_token")
@NoArgsConstructor
@Getter
@Setter
public class DeviceTokenDocument {
  @Id
  private String id;

  @Indexed
  private Integer userId;
  private String token;
  private Instant createdAt;
  @Indexed(expireAfter = "270d")
  private Instant lastUsedAt;

  public DeviceTokenDocument(Integer userId, String token) {
    Instant now = Instant.now();

    this.userId = userId;
    this.token = token;
    this.createdAt = now;
    this.lastUsedAt = now;
  }
}
