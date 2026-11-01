package com.cadence.controller;

import com.cadence.dto.report.*;
import com.cadence.entity.enums.AuditAction;
import com.cadence.security.UserPrincipal;
import com.cadence.service.AuditService;
import com.cadence.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuditService auditService;

    @PostMapping
    @PreAuthorize("hasRole('TEAM_MEMBER')")
    public ResponseEntity<ReportResponseDTO> create(
            @Valid @RequestBody ReportRequestDTO request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createDraft(principal.getUserId(), request));
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('TEAM_MEMBER') and @reportAccessService.isOwner(#reportId, authentication)")
    public ReportResponseDTO update(@PathVariable UUID reportId, @Valid @RequestBody ReportRequestDTO request) {
        return reportService.updateDraft(reportId, request);
    }

    @PostMapping("/{reportId}/submit")
    @PreAuthorize("hasRole('TEAM_MEMBER') and @reportAccessService.isOwner(#reportId, authentication)")
    public ReportResponseDTO submit(@PathVariable UUID reportId) {
        return reportService.submit(reportId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEAM_MEMBER')")
    public Page<ReportSummaryResponseDTO> myHistory(
            @AuthenticationPrincipal UserPrincipal principal, ReportFilterCriteria criteria, Pageable pageable) {
        return reportService.getOwnHistory(principal.getUserId(), criteria, pageable);
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("@reportAccessService.canView(#reportId, authentication)")
    public ReportResponseDTO getDetail(@PathVariable UUID reportId) {
        return reportService.getDetail(reportId);
    }

    @GetMapping("/{reportId}/versions")
    @PreAuthorize("@reportAccessService.canView(#reportId, authentication)")
    public List<ReportVersionResponseDTO> getVersions(@PathVariable UUID reportId) {
        return reportService.getVersions(reportId);
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public Page<ReportSummaryResponseDTO> dashboard(
            ReportFilterCriteria criteria, Pageable pageable, @AuthenticationPrincipal UserPrincipal principal) {
        return reportService.getDashboard(criteria, principal.getUserId(), pageable);
    }

    @PostMapping("/{reportId}/review")
    @PreAuthorize("hasRole('MANAGER') and @reportAccessService.canReview(#reportId, authentication)")
    public ReportResponseDTO review(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest http) {
        ReportResponseDTO result = reportService.review(reportId, request);
        auditService.record(principal.getUserId(), principal.getEmail(),
                request.getDecision() == ReviewDecision.APPROVE
                        ? AuditAction.REPORT_APPROVED
                        : AuditAction.REPORT_CHANGES_REQUESTED,
                "report " + reportId, null, http.getRemoteAddr());
        return result;
    }

    @PatchMapping("/{reportId}/comment")
    @PreAuthorize("hasRole('MANAGER') and @reportAccessService.canReview(#reportId, authentication)")
    public ReportResponseDTO editComment(
            @PathVariable UUID reportId,
            @Valid @RequestBody EditCommentRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest http) {
        ReportResponseDTO result = reportService.editManagerComment(reportId, request.getComment());
        auditService.record(principal.getUserId(), principal.getEmail(), AuditAction.REPORT_COMMENT_EDITED,
                "report " + reportId, null, http.getRemoteAddr());
        return result;
    }
}
