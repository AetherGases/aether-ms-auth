package com.aether.ms_auth.notification.mappers;

import com.aether.ms_auth.notification.dto.output.FindManyNotificationsOutputDTO;
import com.aether.ms_auth.shared.persistence.mongo.documents.NotificationDocument;

import java.util.List;

public class NotificationMapper {
  private NotificationMapper(){}

  public static FindManyNotificationsOutputDTO convertDocumentsToGetNotificationsOutput(
      List<NotificationDocument> documents,
      long total,
      int skip,
      int take
  ){
    return new FindManyNotificationsOutputDTO(
        documents.stream()
            .map(document -> new FindManyNotificationsOutputDTO.Notification(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getInventory() == null ? null : new FindManyNotificationsOutputDTO.Inventory(
                    document.getInventory().getId(),
                    document.getInventory().getStatus(),
                    document.getInventory().getType()
                ),
                document.getCreatedAt()
            ))
            .toList(),
        total,
        skip,
        take
    );
  }
}
