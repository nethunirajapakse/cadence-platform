package com.cadence.controller;

import com.cadence.dto.dashboard.*;
import com.cadence.dto.user.TeamMemberFilterCriteria;
import com.cadence.security.UserPrincipal;
import com.cadence.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Every figure here is computed over the signed-in manager's direct reports only.
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponseDTO getSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getSummary(principal.getUserId());
    }

    @GetMapping("/tasks-trend")
    public List<TasksTrendPointDTO> getTasksCompletedTrend(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getTasksCompletedTrend(principal.getUserId());
    }

    @GetMapping("/status-by-member")
    public List<MemberStatusBreakdownDTO> getStatusByMember(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getStatusByMember(principal.getUserId());
    }

    @GetMapping("/workload-by-project")
    public List<ProjectWorkloadDTO> getWorkloadByProject(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getWorkloadByProject(principal.getUserId());
    }

    @GetMapping("/time-by-task-type")
    public List<TaskTypeHoursDTO> getTimeByTaskType(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getTimeByTaskType(principal.getUserId());
    }

    @GetMapping("/recent-activity")
    public List<ActivityItemDTO> getRecentActivity(
            @RequestParam(defaultValue = "15") int limit, @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getRecentActivity(principal.getUserId(), limit);
    }

    @GetMapping("/member-stats/{userId}")
    public MemberStatsResponseDTO getMemberStats(
            @PathVariable UUID userId, @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getMemberStats(principal.getUserId(), userId);
    }

    @GetMapping("/team-overview")
    public Page<TeamMemberOverviewDTO> getTeamMemberOverview(
            TeamMemberFilterCriteria criteria, Pageable pageable, @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getTeamMemberOverview(principal.getUserId(), criteria, pageable);
    }
}
