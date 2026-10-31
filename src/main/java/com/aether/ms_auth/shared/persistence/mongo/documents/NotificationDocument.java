package com.aether.ms_auth.shared.persistence.mongo.documents;

import com.aether.ms_auth.shared.enums.InventoryStatusEnum;
import com.aether.ms_auth.shared.enums.InventoryTypeEnum;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@NoArgsConstructor
@Getter
@Setter
public class NotificationDocument {
  @Id
  private String id;

  @Indexed
  private Integer userId;
  private Inventory inventory;
  private String title;
  private String description;

  @Indexed(expireAfter = "2d")
  private Instant createdAt;
  class Inventory{
    private Integer id;
    private InventoryStatusEnum status;
    private InventoryTypeEnum type;
  }
}
