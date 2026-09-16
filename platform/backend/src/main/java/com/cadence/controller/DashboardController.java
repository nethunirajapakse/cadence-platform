package com.cadence.controller;

import com.cadence.dto.dashboard.*;
import com.cadence.dto.user.TeamMemberFilterCriteria;
import com.cadence.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponseDTO getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/tasks-trend")
    public List<TasksTrendPointDTO> getTasksCompletedTrend() {
        return dashboardService.getTasksCompletedTrend();
    }

    @GetMapping("/status-by-member")
    public List<MemberStatusBreakdownDTO> getStatusByMember() {
        return dashboardService.getStatusByMember();
    }

    @GetMapping("/workload-by-project")
    public List<ProjectWorkloadDTO> getWorkloadByProject() {
        return dashboardService.getWorkloadByProject();
    }

    @GetMapping("/time-by-task-type")
    public List<TaskTypeHoursDTO> getTimeByTaskType() {
        return dashboardService.getTimeByTaskType();
    }

    @GetMapping("/recent-activity")
    public List<ActivityItemDTO> getRecentActivity(@RequestParam(defaultValue = "15") int limit) {
        return dashboardService.getRecentActivity(limit);
    }

    @GetMapping("/member-stats/{userId}")
    public MemberStatsResponseDTO getMemberStats(@PathVariable UUID userId) {
        return dashboardService.getMemberStats(userId);
    }

    @GetMapping("/team-overview")
    public Page<TeamMemberOverviewDTO> getTeamMemberOverview(TeamMemberFilterCriteria criteria, Pageable pageable) {
        return dashboardService.getTeamMemberOverview(criteria, pageable);
    }
}
