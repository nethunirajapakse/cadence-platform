package com.cadence.repository;

import com.cadence.entity.Blocker;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BlockerRepository extends JpaRepository<Blocker, UUID> {
    List<Blocker> findByReport_ReportId(UUID reportId);

    // "Open blockers" = blockers on reports still actively in flight
    // (SUBMITTED or NEEDS_CORRECTION) - once a report is APPROVED, its
    // blockers are considered resolved/moot; DRAFT is excluded because
    // managers can't see drafts at all.
    @Query("SELECT b FROM Blocker b JOIN FETCH b.report r WHERE r.status IN :statuses")
    List<Blocker> findAllForReportsInStatuses(@Param("statuses") List<ReportStatus> statuses);
}
