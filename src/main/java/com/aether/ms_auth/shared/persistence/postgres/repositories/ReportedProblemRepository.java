package com.aether.ms_auth.shared.persistence.postgres.repositories;

import com.aether.ms_auth.shared.persistence.postgres.entities.ReportedProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportedProblemRepository extends JpaRepository<ReportedProblemEntity, Integer> {
}
