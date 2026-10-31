package com.aether.ms_auth.notification;

import com.aether.ms_auth.notification.dto.input.FindManyNotificationsInputDTO;
import com.aether.ms_auth.notification.dto.output.FindManyNotificationsOutputDTO;
import com.aether.ms_auth.notification.services.NotificationService;
import com.aether.ms_auth.shared.enums.InventoryStatusEnum;
import com.aether.ms_auth.shared.enums.InventoryTypeEnum;
import com.aether.ms_auth.shared.persistence.mongo.documents.NotificationDocument;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTests {
  private static final Integer USER_ID = 7;

  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  private NotificationService notificationService;

  private NotificationDocument document(String id, boolean withInventory) {
    NotificationDocument document = new NotificationDocument();
    ReflectionTestUtils.setField(document, "id", id);
    document.setUserId(USER_ID);
    document.setTitle("titulo " + id);
    document.setDescription("descricao " + id);
    document.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));

    if (withInventory) {
      NotificationDocument.Inventory inventory = new NotificationDocument.Inventory();
      inventory.setId(3);
      inventory.setStatus(InventoryStatusEnum.APPROVED);
      inventory.setType(InventoryTypeEnum.INPUT);
      document.setInventory(inventory);
    }

    return document;
  }

  private Query capturePageQuery() {
    ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(captor.capture(), eq(NotificationDocument.class));
    return captor.getValue();
  }

  @Test
  @DisplayName("Should map documents, total, skip and take")
  void findManySuccess() {
    when(mongoTemplate.count(any(Query.class), eq(NotificationDocument.class))).thenReturn(42L);
    when(mongoTemplate.find(any(Query.class), eq(NotificationDocument.class)))
        .thenReturn(List.of(document("a", true), document("b", false)));

    FindManyNotificationsOutputDTO output =
        notificationService.findManyNotifications(new FindManyNotificationsInputDTO(USER_ID, 10, 5));

    assertEquals(42L, output.total());
    assertEquals(10, output.skip());
    assertEquals(5, output.take());
    assertEquals(2, output.notifications().size());

    FindManyNotificationsOutputDTO.Notification first = output.notifications().get(0);
    assertEquals("a", first.id());
    assertEquals("titulo a", first.title());
    assertEquals("descricao a", first.description());
    assertEquals(Instant.parse("2026-01-01T10:00:00Z"), first.createdAt());
    assertEquals(3, first.inventory().id());
    assertEquals(InventoryStatusEnum.APPROVED, first.inventory().status());
    assertEquals(InventoryTypeEnum.INPUT, first.inventory().type());

    assertNull(output.notifications().get(1).inventory());
  }

  @Test
  @DisplayName("Should filter by user, sort by newest first and apply skip/take")
  void findManyBuildsPageQuery() {
    when(mongoTemplate.count(any(Query.class), eq(NotificationDocument.class))).thenReturn(0L);
    when(mongoTemplate.find(any(Query.class), eq(NotificationDocument.class))).thenReturn(List.of());

    notificationService.findManyNotifications(new FindManyNotificationsInputDTO(USER_ID, 15, 30));

    Query page = capturePageQuery();

    assertEquals(USER_ID, page.getQueryObject().get("userId"));
    assertEquals(-1, page.getSortObject().get("createdAt"));
    assertEquals(15L, page.getSkip());
    assertEquals(30, page.getLimit());
  }

  @Test
  @DisplayName("Should count only the notifications of the user")
  void findManyCountsOnlyUserNotifications() {
    when(mongoTemplate.count(any(Query.class), eq(NotificationDocument.class))).thenReturn(0L);
    when(mongoTemplate.find(any(Query.class), eq(NotificationDocument.class))).thenReturn(List.of());

    notificationService.findManyNotifications(new FindManyNotificationsInputDTO(USER_ID, 0, 20));

    ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).count(captor.capture(), eq(NotificationDocument.class));

    Document filter = captor.getValue().getQueryObject();
    assertEquals(USER_ID, filter.get("userId"));
    assertEquals(0L, captor.getValue().getSkip());
  }

  @Test
  @DisplayName("Should raise a take lower than 1 to 1")
  void findManyRaisesTake() {
    when(mongoTemplate.count(any(Query.class), eq(NotificationDocument.class))).thenReturn(0L);
    when(mongoTemplate.find(any(Query.class), eq(NotificationDocument.class))).thenReturn(List.of());

    FindManyNotificationsOutputDTO output =
        notificationService.findManyNotifications(new FindManyNotificationsInputDTO(USER_ID, 0, 0));

    assertEquals(1, output.take());
    assertEquals(1, capturePageQuery().getLimit());
  }

  @Test
  @DisplayName("Should raise a negative skip to 0")
  void findManyRaisesSkip() {
    when(mongoTemplate.count(any(Query.class), eq(NotificationDocument.class))).thenReturn(0L);
    when(mongoTemplate.find(any(Query.class), eq(NotificationDocument.class))).thenReturn(List.of());

    FindManyNotificationsOutputDTO output =
        notificationService.findManyNotifications(new FindManyNotificationsInputDTO(USER_ID, -5, 20));

    assertEquals(0, output.skip());
    assertEquals(0L, capturePageQuery().getSkip());
  }

  @Test
  @DisplayName("Should return an empty list when the user has no notifications")
  void findManyEmpty() {
    when(mongoTemplate.count(any(Query.class), eq(NotificationDocument.class))).thenReturn(0L);
    when(mongoTemplate.find(any(Query.class), eq(NotificationDocument.class))).thenReturn(List.of());

    FindManyNotificationsOutputDTO output =
        notificationService.findManyNotifications(new FindManyNotificationsInputDTO(USER_ID, 0, 20));

    assertTrue(output.notifications().isEmpty());
    assertEquals(0L, output.total());
  }
}
