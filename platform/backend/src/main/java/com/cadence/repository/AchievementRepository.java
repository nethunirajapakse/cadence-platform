package com.cadence.repository;

import com.cadence.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    List<Achievement> findByReport_ReportId(UUID reportId);
}
