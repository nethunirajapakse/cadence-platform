package com.cadence.service;

import com.cadence.dto.*;
import com.cadence.entity.Blocker;
import com.cadence.entity.ReportTask;
import com.cadence.entity.TimeLog;
import com.cadence.entity.User;
import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.entity.enums.RoleName;
import com.cadence.entity.enums.TaskStatus;
import com.cadence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// All aggregation here happens in Java over a handful of JOIN-FETCHed queries,
// not via SQL GROUP BY. Deliberate choice at this data scale (tens of team
// members, low hundreds of reports/tasks): it's far more readable than
// hand-rolled aggregate JPQL/QueryDSL, and it avoids adding more surface
// area to the QueryDSL setup that's already been fragile on this project. A
// system with real growth would eventually want these pushed into the
// database, but that's not where this assignment's data volume lives.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportTaskRepository reportTaskRepository;
    private final BlockerRepository blockerRepository;
    private final TimeLogRepository timeLogRepository;

    public DashboardSummaryResponse getSummary() {
        LocalDate currentWeekStart = mostRecentMonday();
        LocalDate currentWeekEnd = currentWeekStart.plusDays(4);
        LocalDate today = LocalDate.now();

        List<User> teamMembers = userRepository.findByRoleName(RoleName.TEAM_MEMBER);
        List<WeeklyReport> currentWeekReports = weeklyReportRepository.findByWeekStartDate(currentWeekStart);

        Map<UUID, WeeklyReport> reportByUserId = currentWeekReports.stream()
                .collect(Collectors.toMap(r -> r.getUser().getUserId(), r -> r, (a, b) -> a));

        int submitted = 0;
        int pending = 0;
        int late = 0;

        for (User member : teamMembers) {
            WeeklyReport report = reportByUserId.get(member.getUserId());
            boolean hasSubmitted = report != null && report.getStatus() != ReportStatus.DRAFT;

            if (hasSubmitted) {
                submitted++;
            } else if (today.isAfter(currentWeekEnd)) {
                late++;
            } else {
                pending++;
            }
        }

        long needsCorrectionCount = weeklyReportRepository.countByStatus(ReportStatus.NEEDS_CORRECTION);

        long openBlockersCount = blockerRepository
                .findAllForReportsInStatuses(List.of(ReportStatus.SUBMITTED, ReportStatus.NEEDS_CORRECTION))
                .size();

        return new DashboardSummaryResponse(
                submitted,
                new SubmissionCompliance(submitted, pending, late, teamMembers.size()),
                (int) needsCorrectionCount,
                (int) openBlockersCount);
    }

    // Team-wide count of DONE tasks per week, across the weeks that actually
    // have data - naturally limited to however many weeks of history exist
    // (the seeded data covers about 4).
    public List<TasksTrendPoint> getTasksCompletedTrend() {
        List<ReportTask> tasks = reportTaskRepository.findAllExcludingReportStatus(ReportStatus.DRAFT);

        Map<LocalDate, Long> countsByWeek = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .collect(Collectors.groupingBy(t -> t.getReport().getWeekStartDate(), Collectors.counting()));

        return countsByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new TasksTrendPoint(e.getKey(), e.getValue().intValue()))
                .toList();
    }

    // Deliberately excludes DRAFT - a manager can't see drafts, so a
    // per-member breakdown of "submission/approval status" only makes sense
    // over the statuses they can actually observe.
    public List<MemberStatusBreakdown> getStatusByMember() {
        List<WeeklyReport> reports = weeklyReportRepository.findAllExcludingStatus(ReportStatus.DRAFT);

        Map<String, List<WeeklyReport>> byUser = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getName()));

        return byUser.entrySet().stream()
                .map(e -> {
                    Map<ReportStatus, Long> counts = e.getValue().stream()
                            .collect(Collectors.groupingBy(WeeklyReport::getStatus, Collectors.counting()));
                    return new MemberStatusBreakdown(
                            e.getKey(),
                            counts.getOrDefault(ReportStatus.SUBMITTED, 0L).intValue(),
                            counts.getOrDefault(ReportStatus.NEEDS_CORRECTION, 0L).intValue(),
                            counts.getOrDefault(ReportStatus.APPROVED, 0L).intValue());
                })
                .sorted(Comparator.comparing(MemberStatusBreakdown::getUserName))
                .toList();
    }

    // Task count per project, standing in for "workload" - a project with
    // more logged tasks across the team has more people actively working on it.
    public List<ProjectWorkload> getWorkloadByProject() {
        List<ReportTask> tasks = reportTaskRepository.findAllExcludingReportStatus(ReportStatus.DRAFT);

        Map<String, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getReport().getProject().getName(), Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new ProjectWorkload(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ProjectWorkload::getTaskCount).reversed())
                .toList();
    }

    public List<TaskTypeHours> getTimeByTaskType() {
        List<TimeLog> logs = timeLogRepository.findAllExcludingReportStatus(ReportStatus.DRAFT);

        Map<String, BigDecimal> sums = logs.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTaskType().name(),
                        Collectors.reducing(BigDecimal.ZERO, TimeLog::getHours, BigDecimal::add)));

        return sums.entrySet().stream()
                .map(e -> new TaskTypeHours(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TaskTypeHours::getTotalHours).reversed())
                .toList();
    }

    // Most recent submit/approve/correction actions, newest first - the one
    // place recency actually matters, since submittedAt/approvedAt were
    // deliberately backdated by the seeder to spread across real weeks.
    public List<ActivityItem> getRecentActivity(int limit) {
        List<WeeklyReport> reports = weeklyReportRepository.findAllExcludingStatus(ReportStatus.DRAFT);

        return reports.stream()
                .map(this::toActivityItem)
                .filter(item -> item.getActionAt() != null)
                .sorted(Comparator.comparing(ActivityItem::getActionAt).reversed())
                .limit(limit)
                .toList();
    }

    private ActivityItem toActivityItem(WeeklyReport report) {
        LocalDateTime actionAt = report.getApprovedAt() != null ? report.getApprovedAt() : report.getSubmittedAt();

        String description = switch (report.getStatus()) {
            case APPROVED -> report.getUser().getName() + "'s report was approved";
            case NEEDS_CORRECTION -> report.getUser().getName() + "'s report was sent back for correction";
            case SUBMITTED -> report.getUser().getName() + " submitted a report";
            case DRAFT -> report.getUser().getName() + "'s report is still a draft";
        };

        return new ActivityItem(
                report.getReportId(),
                report.getUser().getName(),
                report.getProject().getName(),
                report.getStatus().name(),
                actionAt,
                description);
    }

    private LocalDate mostRecentMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
