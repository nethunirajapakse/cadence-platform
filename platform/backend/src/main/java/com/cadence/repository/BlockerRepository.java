package com.cadence.repository;

import com.cadence.entity.Blocker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlockerRepository extends JpaRepository<Blocker, UUID> {
    List<Blocker> findByReport_ReportId(UUID reportId);
}
