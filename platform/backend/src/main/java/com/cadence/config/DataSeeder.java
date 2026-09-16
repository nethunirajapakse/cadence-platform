package com.cadence.config;

import com.cadence.config.seed.SeedProject;
import com.cadence.config.seed.SeedReport;
import com.cadence.config.seed.SeedUser;
import com.cadence.dto.report.ReportRequest;
import com.cadence.dto.report.ReviewDecision;
import com.cadence.dto.report.ReviewRequestDTO;
import com.cadence.entity.Project;
import com.cadence.entity.ReportVersion;
import com.cadence.entity.Role;
import com.cadence.entity.User;
import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.RoleName;
import com.cadence.repository.project.ProjectRepository;
import com.cadence.repository.report.ReportVersionRepository;
import com.cadence.repository.user.RoleRepository;
import com.cadence.repository.user.UserRepository;
import com.cadence.repository.report.WeeklyReportRepository;
import com.cadence.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

// Runs on every startup, only inserting what's missing - safe to leave in
// permanently rather than a one-off script. Order matters within run():
// roles -> users -> projects -> reports, since reports reference the other three.
//
// Report weeks are specified in the JSON as "weeksAgo" (an offset from
// whichever Monday the seeder actually runs on), not literal dates, so the
// seeded dataset always reads as "this week" / "last week" / etc. rather than
// going stale between when this was written and when it's actually demoed.
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportService reportService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        seedRoleIfMissing(RoleName.TEAM_MEMBER, "Creates and submits their own weekly reports");
        seedRoleIfMissing(RoleName.MANAGER, "Reviews reports across the whole team and manages projects");

        seedUsersFromJson();
        seedProjectsFromJson();
        seedReportsFromJson();
    }

    // ---- roles ---------------------------------------------------------------

    private void seedRoleIfMissing(RoleName roleName, String description) {
        roleRepository.findByRoleName(roleName).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .roleName(roleName)
                        .description(description)
                        .build()));
    }

    // ---- users -----------------------------------------------------------------

    private void seedUsersFromJson() throws Exception {
        List<SeedUser> seedUsers = readJsonList("seed-data/users.json", SeedUser.class);

        for (SeedUser seedUser : seedUsers) {
            if (userRepository.existsByEmail(seedUser.getEmail())) {
                continue;
            }

            Role role = roleRepository.findByRoleName(RoleName.valueOf(seedUser.getRole()))
                    .orElseThrow(() -> new IllegalStateException("Role not seeded: " + seedUser.getRole()));

            User user = User.builder()
                    .name(seedUser.getName())
                    .email(seedUser.getEmail())
                    .passwordHash(passwordEncoder.encode(seedUser.getPassword()))
                    .role(role)
                    .build();

            userRepository.save(user);
        }
    }

    // ---- projects ----------------------------------------------------------------

    private void seedProjectsFromJson() throws Exception {
        List<SeedProject> seedProjects = readJsonList("seed-data/projects.json", SeedProject.class);

        for (SeedProject seedProject : seedProjects) {
            if (projectRepository.findByName(seedProject.getName()).isPresent()) {
                continue;
            }
            projectRepository.save(Project.builder()
                    .name(seedProject.getName())
                    .description(seedProject.getDescription())
                    .build());
        }
    }

    // ---- reports -------------------------------------------------------------------

    // Guarded at the top level: if ANY report already exists, the whole report
    // seeding step is skipped. This is deliberately coarse - fine for a dev
    // database that only ever holds seed data or a fresh demo, but it means
    // this won't "top up" partial seed data, and it will do nothing at all
    // once real user-created reports exist.
    private void seedReportsFromJson() throws Exception {
        if (weeklyReportRepository.count() > 0) {
            log.info("Reports already exist - skipping report seeding.");
            return;
        }

        List<SeedReport> seedReports = readJsonList("seed-data/reports.json", SeedReport.class);
        LocalDate mostRecentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (SeedReport seedReport : seedReports) {
            seedOneReport(seedReport, mostRecentMonday);
        }
    }

    private void seedOneReport(SeedReport seedReport, LocalDate mostRecentMonday) {
        User user = userRepository.findByEmail(seedReport.getUserEmail())
                .orElseThrow(() -> new IllegalStateException("Seed user not found: " + seedReport.getUserEmail()));
        Project project = projectRepository.findByName(seedReport.getProjectName())
                .orElseThrow(() -> new IllegalStateException("Seed project not found: " + seedReport.getProjectName()));

        LocalDate weekStart = mostRecentMonday.minusWeeks(seedReport.getWeeksAgo());
        LocalDate weekEnd = weekStart.plusDays(4); // Mon-Fri work week

        ReportRequest request = new ReportRequest();
        request.setProjectId(project.getProjectId());
        request.setWeekStartDate(weekStart);
        request.setWeekEndDate(weekEnd);
        request.setNotes(seedReport.getNotes());
        request.setTasks(seedReport.getTasks());
        request.setNextWeekTasks(seedReport.getNextWeekTasks());
        request.setBlockers(seedReport.getBlockers());
        request.setAchievements(seedReport.getAchievements());
        request.setTimeLogs(seedReport.getTimeLogs());
        request.setNoteLinks(seedReport.getNoteLinks());

        UUID reportId = reportService.createDraft(user.getUserId(), request).getReportId();

        switch (seedReport.getTargetStatus()) {
            case "DRAFT" -> { /* nothing further - leave as a draft */ }
            case "SUBMITTED" -> reportService.submit(reportId);
            case "NEEDS_CORRECTION" -> {
                reportService.submit(reportId);
                reportService.review(reportId, reviewRequest(ReviewDecision.REQUEST_CHANGES, seedReport.getReviewComment()));
            }
            case "APPROVED" -> {
                reportService.submit(reportId);
                reportService.review(reportId, reviewRequest(ReviewDecision.APPROVE, seedReport.getApprovalComment()));
            }
            case "APPROVED_AFTER_CORRECTION" -> {
                reportService.submit(reportId);
                reportService.review(reportId, reviewRequest(ReviewDecision.REQUEST_CHANGES, seedReport.getReviewComment()));
                reportService.updateDraft(reportId, request); // resubmission - same content is fine for seed purposes
                reportService.submit(reportId);
                reportService.review(reportId, reviewRequest(ReviewDecision.APPROVE, seedReport.getApprovalComment()));
            }
            default -> throw new IllegalStateException("Unknown targetStatus: " + seedReport.getTargetStatus());
        }

        backdateTimestamps(reportId, weekEnd);
    }

    private ReviewRequestDTO reviewRequest(ReviewDecision decision, String comment) {
        ReviewRequestDTO request = new ReviewRequestDTO();
        request.setDecision(decision);
        request.setComment(comment);
        return request;
    }

    // Everything above happens "now" (createDraft/submit/review all stamp
    // LocalDateTime.now()), which would cluster every seeded report's
    // timestamps at seeder-run-time - unhelpful for a "recent activity" feed
    // or a trend-over-time chart. This spreads them back across the report's
    // actual week instead, directly on the entities, after the real workflow
    // has already produced correct statuses and version history.
    private void backdateTimestamps(UUID reportId, LocalDate weekEnd) {
        WeeklyReport report = weeklyReportRepository.findById(reportId).orElseThrow();
        List<ReportVersion> versions = reportVersionRepository.findByReport_ReportIdOrderByVersionNumberAsc(reportId);

        if (versions.isEmpty()) {
            return; // still DRAFT - nothing was ever submitted
        }

        LocalDateTime firstSubmittedAt = weekEnd.atTime(17, 0); // end of that Friday

        ReportVersion firstVersion = versions.get(0);
        firstVersion.setSubmittedAt(firstSubmittedAt);
        if (firstVersion.getCommentPostedAt() != null) {
            firstVersion.setCommentPostedAt(firstSubmittedAt.plusDays(1));
        }
        reportVersionRepository.save(firstVersion);

        if (versions.size() > 1) {
            LocalDateTime secondSubmittedAt = weekEnd.plusDays(3).atTime(10, 0); // following Monday
            ReportVersion secondVersion = versions.get(1);
            secondVersion.setSubmittedAt(secondSubmittedAt);
            if (secondVersion.getCommentPostedAt() != null) {
                secondVersion.setCommentPostedAt(secondSubmittedAt.plusDays(1));
            }
            reportVersionRepository.save(secondVersion);

            report.setSubmittedAt(secondSubmittedAt);
            if (report.getApprovedAt() != null) {
                report.setApprovedAt(secondSubmittedAt.plusDays(1));
            }
        } else {
            report.setSubmittedAt(firstSubmittedAt);
            if (report.getApprovedAt() != null) {
                report.setApprovedAt(firstSubmittedAt.plusDays(2));
            }
        }

        weeklyReportRepository.save(report);
    }

    // ---- json helper -----------------------------------------------------------

    private <T> List<T> readJsonList(String classpathLocation, Class<T> elementType) throws Exception {
        try (InputStream inputStream = new ClassPathResource(classpathLocation).getInputStream()) {
            var listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(inputStream, listType);
        }
    }
}
