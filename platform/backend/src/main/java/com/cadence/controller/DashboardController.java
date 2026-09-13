package com.cadence.controller;

import com.cadence.dto.*;
import com.cadence.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/tasks-trend")
    public List<TasksTrendPoint> getTasksCompletedTrend() {
        return dashboardService.getTasksCompletedTrend();
    }

    @GetMapping("/status-by-member")
    public List<MemberStatusBreakdown> getStatusByMember() {
        return dashboardService.getStatusByMember();
    }

    @GetMapping("/workload-by-project")
    public List<ProjectWorkload> getWorkloadByProject() {
        return dashboardService.getWorkloadByProject();
    }

    @GetMapping("/time-by-task-type")
    public List<TaskTypeHours> getTimeByTaskType() {
        return dashboardService.getTimeByTaskType();
    }

    @GetMapping("/recent-activity")
    public List<ActivityItem> getRecentActivity(@RequestParam(defaultValue = "15") int limit) {
        return dashboardService.getRecentActivity(limit);
    }
}
