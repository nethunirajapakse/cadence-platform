package com.cadence.service;

import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.repository.report.WeeklyReportRepository;
import com.cadence.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

// Role checks (@PreAuthorize("hasRole('MANAGER')")) only answer "what can this
// role do in general" - they can't know who OWNS a specific report, who that
// owner reports to, or what STATUS it's in, without loading it first. Those
// row-level checks live here.
//
// Visibility rules:
//   - the author can always see their own report, in any status;
//   - a manager can see (and review) a report only if its author is one of
//     that manager's DIRECT REPORTS, and never while it is still a draft;
//   - nobody else can - this includes administrators, who manage accounts
//     but are not given access to report content.
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

    // Used for read access (detail, version history). This must hold everywhere
    // a report can be read, not just on the dashboard list, or a manager could
    // still reach someone else's report by guessing/copying its reportId.
    public boolean canView(UUID reportId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return weeklyReportRepository.findById(reportId)
                .map(report -> {
                    boolean isOwner = report.getUser().getUserId().equals(principal.getUserId());
                    if (isOwner) {
                        return true;
                    }
                    return isManagerOfSubmittedReport(report, reportId, principal);
                })
                .orElse(false);
    }

    // Used for review decisions and comment edits: same rule as a manager's
    // read access - the author's own manager, on a non-draft report.
    public boolean canReview(UUID reportId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return weeklyReportRepository.findById(reportId)
                .map(report -> isManagerOfSubmittedReport(report, reportId, principal))
                .orElse(false);
    }

    private boolean isManagerOfSubmittedReport(WeeklyReport report, UUID reportId, UserPrincipal principal) {
        if (!"MANAGER".equals(principal.getRoleName())) {
            return false;
        }
        if (report.getStatus() == ReportStatus.DRAFT) {
            return false;
        }
        return weeklyReportRepository.existsByReportIdAndUser_Manager_UserId(reportId, principal.getUserId());
    }
}
