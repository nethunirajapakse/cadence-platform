package com.cadence.service;

import com.cadence.entity.Role;
import com.cadence.entity.User;
import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.entity.enums.RoleName;
import com.cadence.repository.report.WeeklyReportRepository;
import com.cadence.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Plain Mockito unit test, no Spring context - this is deliberately the
// opposite of RbacEndToEndTest: it proves the access-decision LOGIC in
// isOwner()/canView() is correct in isolation, with nothing else (SecurityConfig,
// cookies, the servlet filter chain) able to interfere with or mask the result.
// Every User/WeeklyReport method call mocked below is copied directly from
// what ReportAccessService.java and UserPrincipal.java actually call - none
// of it is guessed at.
@ExtendWith(MockitoExtension.class)
class ReportAccessServiceTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    private ReportAccessService service() {
        return new ReportAccessService(weeklyReportRepository);
    }

    // ---- test fixtures ---------------------------------------------------

    private UserPrincipal principal(UUID userId, RoleName roleName) {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getUserId()).thenReturn(userId);
        when(user.getName()).thenReturn("Test User");
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getPasswordHash()).thenReturn("hashed-password");
        when(user.isActive()).thenReturn(true);
        when(user.getRole()).thenReturn(role);
        when(role.getRoleName()).thenReturn(roleName);
        return new UserPrincipal(user);
    }

    private Authentication authenticationFor(UserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
    }

    private WeeklyReport reportOwnedBy(UUID ownerId, ReportStatus status) {
        User owner = mock(User.class);
        when(owner.getUserId()).thenReturn(ownerId);

        WeeklyReport report = mock(WeeklyReport.class);
        when(report.getUser()).thenReturn(owner);
        // lenient: getStatus() is only actually called on the "not owner AND
        // is manager" path (short-circuit evaluation in canView, and
        // isOwner() never calls it at all) - Mockito's strict-stub checker
        // correctly flags it as unused on every other path, which is exactly
        // the short-circuit behavior this suite is confirming, not a bug.
        lenient().when(report.getStatus()).thenReturn(status);
        return report;
    }

    // =======================================================================
    // isOwner
    // =======================================================================

    @Test
    void isOwner_trueWhenAuthenticatedUserOwnsTheReport() {
        UUID reportId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(userId, ReportStatus.DRAFT);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(userId, RoleName.TEAM_MEMBER));

        assertThat(service().isOwner(reportId, auth)).isTrue();
    }

    @Test
    void isOwner_falseWhenAnotherUserOwnsTheReport() {
        UUID reportId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(ownerId, ReportStatus.DRAFT);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(requesterId, RoleName.TEAM_MEMBER));

        assertThat(service().isOwner(reportId, auth)).isFalse();
    }

    @Test
    void isOwner_falseWhenReportDoesNotExist() {
        UUID reportId = UUID.randomUUID();
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.empty());

        Authentication auth = authenticationFor(principal(UUID.randomUUID(), RoleName.TEAM_MEMBER));

        assertThat(service().isOwner(reportId, auth)).isFalse();
    }

    // =======================================================================
    // canView
    // =======================================================================

    @Test
    void canView_ownerCanViewTheirOwnDraft() {
        UUID reportId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(userId, ReportStatus.DRAFT);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(userId, RoleName.TEAM_MEMBER));

        assertThat(service().canView(reportId, auth)).isTrue();
    }

    @Test
    void canView_ownerCanViewTheirOwnApprovedReport() {
        UUID reportId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(userId, ReportStatus.APPROVED);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(userId, RoleName.TEAM_MEMBER));

        assertThat(service().canView(reportId, auth)).isTrue();
    }

    @Test
    void canView_falseWhenAnotherTeamMemberTriesToView() {
        UUID reportId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();

        // Even a non-draft, submitted report must stay invisible to a peer -
        // only the owner and a manager (per the rules below) may see it.
        WeeklyReport report = reportOwnedBy(ownerId, ReportStatus.SUBMITTED);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(otherMemberId, RoleName.TEAM_MEMBER));

        assertThat(service().canView(reportId, auth)).isFalse();
    }

    @Test
    void canView_managerIsBlockedFromAnotherUsersDraft() {
        // This is the specific rule the spec calls out by name: "Draft ...
        // only visible to [the owner]" - a manager must be denied even
        // though they otherwise have broad read access.
        UUID reportId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(ownerId, ReportStatus.DRAFT);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(managerId, RoleName.MANAGER));

        assertThat(service().canView(reportId, auth)).isFalse();
    }

    @Test
    void canView_managerCanViewASubmittedReport() {
        UUID reportId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(ownerId, ReportStatus.SUBMITTED);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(managerId, RoleName.MANAGER));

        assertThat(service().canView(reportId, auth)).isTrue();
    }

    @Test
    void canView_managerCanViewAReportNeedingCorrection() {
        UUID reportId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(ownerId, ReportStatus.NEEDS_CORRECTION);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(managerId, RoleName.MANAGER));

        assertThat(service().canView(reportId, auth)).isTrue();
    }

    @Test
    void canView_managerCanViewAnApprovedReport() {
        UUID reportId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        WeeklyReport report = reportOwnedBy(ownerId, ReportStatus.APPROVED);
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        Authentication auth = authenticationFor(principal(managerId, RoleName.MANAGER));

        assertThat(service().canView(reportId, auth)).isTrue();
    }

    @Test
    void canView_falseWhenReportDoesNotExist() {
        UUID reportId = UUID.randomUUID();
        when(weeklyReportRepository.findById(reportId)).thenReturn(Optional.empty());

        Authentication auth = authenticationFor(principal(UUID.randomUUID(), RoleName.MANAGER));

        assertThat(service().canView(reportId, auth)).isFalse();
    }
}
