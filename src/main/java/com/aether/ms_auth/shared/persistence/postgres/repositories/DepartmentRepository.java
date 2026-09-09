package com.aether.ms_auth.shared.persistence.postgres.repositories;

import com.aether.ms_auth.shared.persistence.postgres.entities.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer> {
}
