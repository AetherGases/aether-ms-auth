package com.aether.ms_auth.notification.services;

import com.aether.ms_auth.notification.dto.input.FindManyNotificationsInputDTO;
import com.aether.ms_auth.notification.dto.output.FindManyNotificationsOutputDTO;
import com.aether.ms_auth.notification.mappers.NotificationMapper;
import com.aether.ms_auth.shared.persistence.mongo.documents.NotificationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final MongoTemplate mongoTemplate;

  public FindManyNotificationsOutputDTO findManyNotifications(FindManyNotificationsInputDTO input){
    int take = Math.max(input.take(), 1);
    int skip = Math.max(input.skip(), 0);

    Query filter = Query.query(Criteria.where("userId").is(input.userId()));
    long total = mongoTemplate.count(filter, NotificationDocument.class);

    Query page = Query.of(filter)
        .with(Sort.by(Sort.Direction.DESC, "createdAt"))
        .skip(skip)
        .limit(take);

    List<NotificationDocument> notifications = mongoTemplate.find(page, NotificationDocument.class);

    return NotificationMapper.convertDocumentsToGetNotificationsOutput(notifications, total, skip, take);
  }
}
