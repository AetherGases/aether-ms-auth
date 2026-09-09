package com.aether.ms_auth.shared.persistence.postgres.repositories;

import com.aether.ms_auth.shared.persistence.postgres.entities.StorageFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageFileRepository extends JpaRepository<StorageFileEntity, Integer> {
}
