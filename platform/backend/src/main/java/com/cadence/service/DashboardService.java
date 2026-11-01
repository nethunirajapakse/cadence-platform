package com.cadence.service;

import com.cadence.dto.dashboard.*;
import com.cadence.dto.user.TeamMemberFilterCriteria;
import com.cadence.entity.ReportTask;
import com.cadence.entity.TimeLog;
import com.cadence.entity.User;
import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.entity.enums.TaskStatus;
import com.cadence.repository.report.WeeklyReportRepository;
import com.cadence.repository.report.item.BlockerRepository;
import com.cadence.repository.report.item.ReportTaskRepository;
import com.cadence.repository.report.item.TimeLogRepository;
import com.cadence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Every method takes the signed-in manager's id and computes over THAT manager's
// direct reports only. The scope is resolved here from the id (which comes from
// the authenticated principal), never from anything the client sends.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportTaskRepository reportTaskRepository;
    private final BlockerRepository blockerRepository;
    private final TimeLogRepository timeLogRepository;

    private Set<UUID> scopeOf(UUID managerId) {
        return new HashSet<>(userRepository.findDirectReportIds(managerId));
    }

    public DashboardSummaryResponseDTO getSummary(UUID managerId) {
        LocalDate currentWeekStart = mostRecentMonday();
        LocalDate currentWeekEnd = currentWeekStart.plusDays(4);
        LocalDate today = LocalDate.now();

        Set<UUID> scope = scopeOf(managerId);
        // Deactivated people are not expected to submit, so they don't count as late.
        List<User> teamMembers = userRepository.findActiveDirectReports(managerId);
        List<WeeklyReport> currentWeekReports = weeklyReportRepository.findByWeekStartDate(currentWeekStart).stream()
                .filter(r -> scope.contains(r.getUser().getUserId()))
                .toList();

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

        long needsCorrectionCount = scope.isEmpty()
                ? 0
                : weeklyReportRepository.countByStatusAndUser_UserIdIn(ReportStatus.NEEDS_CORRECTION, scope);

        long openBlockersCount = blockerRepository
                .findAllForReportsInStatuses(List.of(ReportStatus.SUBMITTED, ReportStatus.NEEDS_CORRECTION))
                .stream()
                .filter(b -> scope.contains(b.getReport().getUser().getUserId()))
                .count();

        return new DashboardSummaryResponseDTO(
                submitted,
                new SubmissionComplianceDTO(submitted, pending, late, teamMembers.size()),
                (int) needsCorrectionCount,
                (int) openBlockersCount);
    }

    public List<TasksTrendPointDTO> getTasksCompletedTrend(UUID managerId) {
        Set<UUID> scope = scopeOf(managerId);
        List<ReportTask> tasks = reportTaskRepository.findAllExcludingReportStatus(ReportStatus.DRAFT).stream()
                .filter(t -> scope.contains(t.getReport().getUser().getUserId()))
                .toList();

        Map<LocalDate, Long> countsByWeek = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .collect(Collectors.groupingBy(t -> t.getReport().getWeekStartDate(), Collectors.counting()));

        return countsByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new TasksTrendPointDTO(e.getKey(), e.getValue().intValue()))
                .toList();
    }

    public List<MemberStatusBreakdownDTO> getStatusByMember(UUID managerId) {
        Set<UUID> scope = scopeOf(managerId);
        List<WeeklyReport> reports = weeklyReportRepository.findAllExcludingStatus(ReportStatus.DRAFT).stream()
                .filter(r -> scope.contains(r.getUser().getUserId()))
                .toList();

        Map<String, List<WeeklyReport>> byUser = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getName()));

        return byUser.entrySet().stream()
                .map(e -> {
                    Map<ReportStatus, Long> counts = e.getValue().stream()
                            .collect(Collectors.groupingBy(WeeklyReport::getStatus, Collectors.counting()));
                    return new MemberStatusBreakdownDTO(
                            e.getKey(),
                            counts.getOrDefault(ReportStatus.SUBMITTED, 0L).intValue(),
                            counts.getOrDefault(ReportStatus.NEEDS_CORRECTION, 0L).intValue(),
                            counts.getOrDefault(ReportStatus.APPROVED, 0L).intValue());
                })
                .sorted(Comparator.comparing(MemberStatusBreakdownDTO::getUserName))
                .toList();
    }

    public List<ProjectWorkloadDTO> getWorkloadByProject(UUID managerId) {
        Set<UUID> scope = scopeOf(managerId);
        List<ReportTask> tasks = reportTaskRepository.findAllExcludingReportStatus(ReportStatus.DRAFT).stream()
                .filter(t -> scope.contains(t.getReport().getUser().getUserId()))
                .toList();

        Map<String, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getReport().getProject().getName(), Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new ProjectWorkloadDTO(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ProjectWorkloadDTO::getTaskCount).reversed())
                .toList();
    }

    public List<TaskTypeHoursDTO> getTimeByTaskType(UUID managerId) {
        Set<UUID> scope = scopeOf(managerId);
        List<TimeLog> logs = timeLogRepository.findAllExcludingReportStatus(ReportStatus.DRAFT).stream()
                .filter(l -> scope.contains(l.getReport().getUser().getUserId()))
                .toList();

        Map<String, BigDecimal> sums = logs.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTaskType().name(),
                        Collectors.reducing(BigDecimal.ZERO, TimeLog::getHours, BigDecimal::add)));

        return sums.entrySet().stream()
                .map(e -> new TaskTypeHoursDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TaskTypeHoursDTO::getTotalHours).reversed())
                .toList();
    }

    public List<ActivityItemDTO> getRecentActivity(UUID managerId, int limit) {
        Set<UUID> scope = scopeOf(managerId);
        List<WeeklyReport> reports = weeklyReportRepository.findAllExcludingStatus(ReportStatus.DRAFT).stream()
                .filter(r -> scope.contains(r.getUser().getUserId()))
                .toList();

        return reports.stream()
                .map(this::toActivityItem)
                .filter(item -> item.getActionAt() != null)
                .sorted(Comparator.comparing(ActivityItemDTO::getActionAt).reversed())
                .limit(limit)
                .toList();
    }

    // Backs the team-member profile page's stat cards - the same shape of
    // metric as the team-wide summary, but scoped down to one person. DRAFT
    // reports are excluded here too, for the same reason a manager can't see
    // them anywhere else: they're not "this person's activity" from a
    // manager's point of view until submitted.
    public MemberStatsResponseDTO getMemberStats(UUID managerId, UUID userId) {
        // Someone else's team member is reported exactly like a user that doesn't exist.
        if (!userRepository.isDirectReportOf(userId, managerId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        List<WeeklyReport> allReports = weeklyReportRepository
                .findByUser_UserId(userId, Pageable.unpaged())
                .getContent();

        List<WeeklyReport> visible = allReports.stream()
                .filter(r -> r.getStatus() != ReportStatus.DRAFT)
                .toList();

        int approved = (int) visible.stream().filter(r -> r.getStatus() == ReportStatus.APPROVED).count();
        int needsCorrection = (int) visible.stream().filter(r -> r.getStatus() == ReportStatus.NEEDS_CORRECTION).count();

        int tasksCompleted = (int) visible.stream()
                .flatMap(r -> r.getTasks().stream())
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();

        int openBlockers = (int) visible.stream()
                .filter(r -> r.getStatus() == ReportStatus.SUBMITTED || r.getStatus() == ReportStatus.NEEDS_CORRECTION)
                .flatMap(r -> r.getBlockers().stream())
                .count();

        return new MemberStatsResponseDTO(visible.size(), approved, needsCorrection, tasksCompleted, openBlockers);
    }

    // Backs the paginated "Team members" list page. Filters and pages the
    // USERS first at the database layer (via QueryDSL, same reasoning as the
    // Projects search - dynamic predicates instead of a null-guarded JPQL
    // string), then computes report stats only for whichever page of users
    // came back - not the whole team on every request.
    public Page<TeamMemberOverviewDTO> getTeamMemberOverview(
            UUID managerId, TeamMemberFilterCriteria criteria, Pageable pageable) {
        List<UUID> scope = userRepository.findDirectReportIds(managerId);
        if (scope.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<User> userPage = userRepository.findTeamMembersByFilters(criteria, scope, pageable);

        List<UUID> userIds = userPage.getContent().stream().map(User::getUserId).toList();
        List<WeeklyReport> reports = userIds.isEmpty()
                ? List.of()
                : weeklyReportRepository.findByUser_UserIdInAndStatusNot(userIds, ReportStatus.DRAFT);

        Map<UUID, List<WeeklyReport>> reportsByUserId = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getUserId()));

        List<TeamMemberOverviewDTO> overview = userPage.getContent().stream()
                .map(member -> {
                    List<WeeklyReport> memberReports = reportsByUserId.getOrDefault(member.getUserId(), List.of());
                    int approved = (int) memberReports.stream().filter(r -> r.getStatus() == ReportStatus.APPROVED).count();
                    int needsCorrection = (int) memberReports.stream()
                            .filter(r -> r.getStatus() == ReportStatus.NEEDS_CORRECTION)
                            .count();

                    return new TeamMemberOverviewDTO(
                            member.getUserId(),
                            member.getName(),
                            member.getEmail(),
                            member.getRole().getRoleName().name(),
                            memberReports.size(),
                            approved,
                            needsCorrection);
                })
                .toList();

        return new PageImpl<>(overview, pageable, userPage.getTotalElements());
    }

    private ActivityItemDTO toActivityItem(WeeklyReport report) {
        LocalDateTime actionAt = report.getApprovedAt() != null ? report.getApprovedAt() : report.getSubmittedAt();

        String description = switch (report.getStatus()) {
            case APPROVED -> report.getUser().getName() + "'s report was approved";
            case NEEDS_CORRECTION -> report.getUser().getName() + "'s report was sent back for correction";
            case SUBMITTED -> report.getUser().getName() + " submitted a report";
            case DRAFT -> report.getUser().getName() + "'s report is still a draft";
        };

        return new ActivityItemDTO(
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
