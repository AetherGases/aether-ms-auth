package com.aether.ms_auth.notification.dto.output;

import com.aether.ms_auth.shared.enums.InventoryStatusEnum;
import com.aether.ms_auth.shared.enums.InventoryTypeEnum;

import java.time.Instant;
import java.util.List;

public record FindManyNotificationsOutputDTO(
    List<Notification> notifications,
    long total,
    int skip,
    int take
) {
  public record Notification(
      String id,
      String title,
      String description,
      Inventory inventory,
      Instant createdAt
  ){}

  public record Inventory(
      Integer id,
      InventoryStatusEnum status,
      InventoryTypeEnum type
  ){}
}
