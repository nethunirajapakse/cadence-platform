package com.cadence.repository.report;

import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, UUID>, WeeklyReportRepositoryCustom {

    Page<WeeklyReport> findByUser_UserId(UUID userId, Pageable pageable);

    List<WeeklyReport> findByWeekStartDate(LocalDate weekStartDate);

    long countByStatus(ReportStatus status);

    List<WeeklyReport> findByUser_UserIdInAndStatusNot(List<UUID> userIds, ReportStatus status);

    @Query("SELECT r FROM WeeklyReport r JOIN FETCH r.user JOIN FETCH r.project WHERE r.status <> :excludedStatus")
    List<WeeklyReport> findAllExcludingStatus(@Param("excludedStatus") ReportStatus excludedStatus);
}
