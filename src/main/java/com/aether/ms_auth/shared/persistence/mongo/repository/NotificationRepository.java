package com.aether.ms_auth.shared.persistence.mongo.repository;


import com.aether.ms_auth.shared.persistence.mongo.documents.NotificationConfigDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository
    extends MongoRepository<NotificationConfigDocument, String> {
}
