package com.cadence.repository.report;

import com.cadence.entity.ReportVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportVersionRepository extends JpaRepository<ReportVersion, UUID> {

    // Full version history for a report, oldest first - what the "past versions" list view reads from.
    List<ReportVersion> findByReport_ReportIdOrderByVersionNumberAsc(UUID reportId);

    // The version currently under review - used to attach a manager's comment to the right one.
    Optional<ReportVersion> findByReport_ReportIdAndCurrentTrue(UUID reportId);
}
