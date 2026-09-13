package com.cadence.controller;

import com.cadence.dto.*;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.security.UserPrincipal;
import com.cadence.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ---- team member: create / edit / submit --------------------------------

    @PostMapping
    @PreAuthorize("hasRole('TEAM_MEMBER')")
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody ReportRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createDraft(principal.getUserId(), request));
    }

    // Ownership-checked: a team member may only edit their OWN report, and only
    // while it's DRAFT/NEEDS_CORRECTION (the service enforces the status rule;
    // @PreAuthorize enforces ownership before the service is even called).
    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('TEAM_MEMBER') and @reportAccessService.isOwner(#reportId, authentication)")
    public ReportResponse update(@PathVariable UUID reportId, @Valid @RequestBody ReportRequest request) {
        return reportService.updateDraft(reportId, request);
    }

    @PostMapping("/{reportId}/submit")
    @PreAuthorize("hasRole('TEAM_MEMBER') and @reportAccessService.isOwner(#reportId, authentication)")
    public ReportResponse submit(@PathVariable UUID reportId) {
        return reportService.submit(reportId);
    }

    // ---- team member: own history -------------------------------------------

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEAM_MEMBER')")
    public Page<ReportSummaryResponse> myHistory(
            @AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return reportService.getOwnHistory(principal.getUserId(), pageable);
    }

    // ---- shared: detail + version history (owner OR any manager) ------------

    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('MANAGER') or @reportAccessService.isOwner(#reportId, authentication)")
    public ReportResponse getDetail(@PathVariable UUID reportId) {
        return reportService.getDetail(reportId);
    }

    @GetMapping("/{reportId}/versions")
    @PreAuthorize("hasRole('MANAGER') or @reportAccessService.isOwner(#reportId, authentication)")
    public List<ReportVersionResponse> getVersions(@PathVariable UUID reportId) {
        return reportService.getVersions(reportId);
    }

    // ---- manager: dashboard + review -----------------------------------------

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<ReportSummaryResponse> dashboard(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) LocalDate weekStart,
            @RequestParam(required = false) LocalDate weekEnd,
            Pageable pageable) {
        return reportService.getDashboard(userId, projectId, status, weekStart, weekEnd, pageable);
    }

    @PostMapping("/{reportId}/review")
    @PreAuthorize("hasRole('MANAGER')")
    public ReportResponse review(@PathVariable UUID reportId, @Valid @RequestBody ReviewRequest request) {
        return reportService.review(reportId, request);
    }
}
