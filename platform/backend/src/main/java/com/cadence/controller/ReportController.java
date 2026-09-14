package com.cadence.controller;

import com.cadence.dto.*;
import com.cadence.security.UserPrincipal;
import com.cadence.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // ---- shared: detail + version history (owner, or manager on non-draft) --

    @GetMapping("/{reportId}")
    @PreAuthorize("@reportAccessService.canView(#reportId, authentication)")
    public ReportResponse getDetail(@PathVariable UUID reportId) {
        return reportService.getDetail(reportId);
    }

    @GetMapping("/{reportId}/versions")
    @PreAuthorize("@reportAccessService.canView(#reportId, authentication)")
    public List<ReportVersionResponse> getVersions(@PathVariable UUID reportId) {
        return reportService.getVersions(reportId);
    }

    // ---- manager: dashboard + review -----------------------------------------

    // criteria is populated by Spring MVC's implicit @ModelAttribute binding -
    // its fields (userId, projectIds, excludeProjects, statuses, weekStart,
    // weekEnd) map directly onto matching query parameter names.
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<ReportSummaryResponse> dashboard(ReportFilterCriteria criteria, Pageable pageable) {
        return reportService.getDashboard(criteria, pageable);
    }

    @PostMapping("/{reportId}/review")
    @PreAuthorize("hasRole('MANAGER')")
    public ReportResponse review(@PathVariable UUID reportId, @Valid @RequestBody ReviewRequest request) {
        return reportService.review(reportId, request);
    }

    @PatchMapping("/{reportId}/comment")
    @PreAuthorize("hasRole('MANAGER')")
    public ReportResponse editComment(@PathVariable UUID reportId, @Valid @RequestBody EditCommentRequest request) {
        return reportService.editManagerComment(reportId, request.getComment());
    }
}
