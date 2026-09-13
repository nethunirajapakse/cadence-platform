package com.cadence.service;

import com.cadence.entity.WeeklyReport;
import com.cadence.repository.WeeklyReportRepository;
import com.cadence.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

// Role checks (@PreAuthorize("hasRole('MANAGER')")) only answer "what can this
// role do in general" - they can't know who OWNS a specific report without
// loading it first. That row-level check lives here, and gets called from
// @PreAuthorize via SpEL: @reportAccessService.isOwner(#reportId, authentication).
//
// This is what actually enforces "a team member must never be able to access
// another team member's report data" - the role check alone doesn't cover it.
@Service
@RequiredArgsConstructor
public class ReportAccessService {

    private final WeeklyReportRepository weeklyReportRepository;

    public boolean isOwner(UUID reportId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return weeklyReportRepository.findById(reportId)
                .map(WeeklyReport::getUser)
                .map(user -> user.getUserId().equals(principal.getUserId()))
                .orElse(false);
    }
}
