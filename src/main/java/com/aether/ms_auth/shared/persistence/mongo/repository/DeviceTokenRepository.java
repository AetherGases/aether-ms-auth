package com.aether.ms_auth.shared.persistence.mongo.repository;


import com.aether.ms_auth.shared.persistence.mongo.documents.DeviceTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;

public interface DeviceTokenRepository
    extends MongoRepository<DeviceTokenDocument, String> {

  @Query("{ 'userId': ?0 }")
  @Update("{ '$set': { 'lastUsedAt': ?1 } }")
  long updateLastUsedAtByUserId(Integer userId, Instant lastUsedAt);
}
