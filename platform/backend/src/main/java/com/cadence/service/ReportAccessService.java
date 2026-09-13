package com.cadence.service;

import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.repository.WeeklyReportRepository;
import com.cadence.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

// Role checks (@PreAuthorize("hasRole('MANAGER')")) only answer "what can this
// role do in general" - they can't know who OWNS a specific report, or what
// STATUS it's in, without loading it first. Both row-level checks live here.
@Service
@RequiredArgsConstructor
public class ReportAccessService {

    private final WeeklyReportRepository weeklyReportRepository;

    // Used for actions a team member takes on their OWN report (edit, submit).
    public boolean isOwner(UUID reportId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return weeklyReportRepository.findById(reportId)
                .map(WeeklyReport::getUser)
                .map(user -> user.getUserId().equals(principal.getUserId()))
                .orElse(false);
    }

    // Used for read access (detail, version history): the owner can always see
    // their own report regardless of status. A manager can see any report
    // EXCEPT a draft - "Draft ... only visible to [the team member]" per the
    // spec. This must hold everywhere a report can be read, not just on the
    // dashboard list, or a manager could still reach a draft by guessing/
    // copying its reportId directly.
    public boolean canView(UUID reportId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return weeklyReportRepository.findById(reportId)
                .map(report -> {
                    boolean isOwner = report.getUser().getUserId().equals(principal.getUserId());
                    if (isOwner) {
                        return true;
                    }
                    boolean isManager = "MANAGER".equals(principal.getRoleName());
                    return isManager && report.getStatus() != ReportStatus.DRAFT;
                })
                .orElse(false);
    }
}
