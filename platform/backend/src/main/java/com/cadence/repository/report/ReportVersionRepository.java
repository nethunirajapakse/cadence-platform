package com.cadence.repository.report;

import com.cadence.entity.ReportVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportVersionRepository extends JpaRepository<ReportVersion, UUID> {

    List<ReportVersion> findByReport_ReportIdOrderByVersionNumberAsc(UUID reportId);

    Optional<ReportVersion> findByReport_ReportIdAndCurrentTrue(UUID reportId);
}
