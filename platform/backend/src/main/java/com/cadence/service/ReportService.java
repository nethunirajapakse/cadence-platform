package com.cadence.service;

import com.cadence.dto.report.*;
import com.cadence.dto.report.item.*;
import com.cadence.entity.*;
import com.cadence.entity.enums.ReportStatus;
import com.cadence.repository.project.ProjectRepository;
import com.cadence.repository.report.ReportVersionRepository;
import com.cadence.repository.report.WeeklyReportRepository;
import com.cadence.repository.user.UserRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final long COMMENT_EDIT_WINDOW_MINUTES = 15;

    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ---- create / edit -----------------------------------------------------

    public ReportResponseDTO createDraft(UUID userId, ReportRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        WeeklyReport report = WeeklyReport.builder()
                .user(user)
                .project(project)
                .weekStartDate(request.getWeekStartDate())
                .weekEndDate(request.getWeekEndDate())
                .notes(request.getNotes())
                .status(ReportStatus.DRAFT)
                .build();

        applyChildCollections(report, request);
        weeklyReportRepository.save(report);

        return toResponse(report);
    }

    public ReportResponseDTO updateDraft(UUID reportId, ReportRequestDTO request) {
        WeeklyReport report = findOrThrow(reportId);

        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new IllegalStateException(
                    "Only reports in DRAFT or NEEDS_CORRECTION can be edited (current status: " + report.getStatus() + ")");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        report.setProject(project);
        report.setWeekStartDate(request.getWeekStartDate());
        report.setWeekEndDate(request.getWeekEndDate());
        report.setNotes(request.getNotes());

        applyChildCollections(report, request);
        weeklyReportRepository.save(report);

        return toResponse(report);
    }

    // ---- submit / review ---------------------------------------------------

    public ReportResponseDTO submit(UUID reportId) {
        WeeklyReport report = findOrThrow(reportId);

        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new IllegalStateException(
                    "Only reports in DRAFT or NEEDS_CORRECTION can be submitted (current status: " + report.getStatus() + ")");
        }

        snapshotVersion(report);

        report.setStatus(ReportStatus.SUBMITTED);
        report.setSubmittedAt(LocalDateTime.now());
        weeklyReportRepository.save(report);

        return toResponse(report);
    }

    public ReportResponseDTO review(UUID reportId, ReviewRequestDTO request) {
        WeeklyReport report = findOrThrow(reportId);

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only SUBMITTED reports can be reviewed (current status: " + report.getStatus() + ")");
        }

        ReportVersion currentVersion = reportVersionRepository.findByReport_ReportIdAndCurrentTrue(reportId)
                .orElseThrow(() -> new IllegalStateException(
                        "No version on record for this report - it may have been submitted before versioning was added"));

        if (request.getDecision() == ReviewDecision.APPROVE) {
            report.setStatus(ReportStatus.APPROVED);
            report.setApprovedAt(LocalDateTime.now());
            applyComment(currentVersion, request.getComment());
        } else {
            if (request.getComment() == null || request.getComment().isBlank()) {
                throw new IllegalStateException("A comment is required when requesting changes");
            }
            report.setStatus(ReportStatus.NEEDS_CORRECTION);
            report.setManagerComment(request.getComment());
            applyComment(currentVersion, request.getComment());
        }

        reportVersionRepository.save(currentVersion);
        weeklyReportRepository.save(report);

        return toResponse(report);
    }

    private void applyComment(ReportVersion version, String comment) {
        version.setComment(comment);
        if (comment != null && !comment.isBlank()) {
            version.setCommentPostedAt(LocalDateTime.now());
            version.setCommentEdited(false);
        }
    }

    public ReportResponseDTO editManagerComment(UUID reportId, String newComment) {
        WeeklyReport report = findOrThrow(reportId);

        if (report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new IllegalStateException(
                    "The manager comment can only be edited while the report is in NEEDS_CORRECTION (current status: "
                            + report.getStatus() + ")");
        }

        ReportVersion currentVersion = reportVersionRepository.findByReport_ReportIdAndCurrentTrue(reportId)
                .orElseThrow(() -> new IllegalStateException("No version on record for this report"));

        if (currentVersion.getCommentPostedAt() == null) {
            throw new IllegalStateException("There is no comment to edit yet");
        }

        long minutesSincePosted = Duration.between(currentVersion.getCommentPostedAt(), LocalDateTime.now()).toMinutes();
        if (minutesSincePosted >= COMMENT_EDIT_WINDOW_MINUTES) {
            throw new IllegalStateException(
                    "The " + COMMENT_EDIT_WINDOW_MINUTES + "-minute edit window for this comment has passed");
        }

        report.setManagerComment(newComment);
        currentVersion.setComment(newComment);
        currentVersion.setCommentEdited(true);

        weeklyReportRepository.save(report);
        reportVersionRepository.save(currentVersion);

        return toResponse(report);
    }

    // ---- reads --------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportResponseDTO getDetail(UUID reportId) {
        return toResponse(findOrThrow(reportId));
    }

    @Transactional(readOnly = true)
    public Page<ReportSummaryResponseDTO> getOwnHistory(UUID userId, ReportFilterCriteria criteria, Pageable pageable) {
        return weeklyReportRepository.findForOwnHistory(userId, criteria, pageable)
                .map(ReportSummaryResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<ReportSummaryResponseDTO> getDashboard(ReportFilterCriteria criteria, Pageable pageable) {
        return weeklyReportRepository.findForDashboard(criteria, pageable)
                .map(ReportSummaryResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public List<ReportVersionResponseDTO> getVersions(UUID reportId) {
        return reportVersionRepository.findByReport_ReportIdOrderByVersionNumberAsc(reportId).stream()
                .map(ReportVersionResponseDTO::new)
                .toList();
    }

    // ---- internal helpers -----------------------------------------------------

    private WeeklyReport findOrThrow(UUID reportId) {
        return weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    }

    private void applyChildCollections(WeeklyReport report, ReportRequestDTO request) {
        report.getTasks().clear();
        request.getTasks().forEach(dto -> report.getTasks().add(
                ReportTask.builder()
                        .report(report)
                        .taskName(dto.getTaskName())
                        .priority(dto.getPriority())
                        .plannedPct(dto.getPlannedPct())
                        .actualPct(dto.getActualPct())
                        .status(dto.getStatus())
                        .timePlanned(dto.getTimePlanned())
                        .timeSpent(dto.getTimeSpent())
                        .deliverable(dto.getDeliverable())
                        .build()));

        report.getNextWeekTasks().clear();
        if (request.getNextWeekTasks() != null) {
            request.getNextWeekTasks().forEach(dto -> report.getNextWeekTasks().add(
                    NextWeekTask.builder()
                            .report(report)
                            .taskDescription(dto.getTaskDescription())
                            .priority(dto.getPriority())
                            .build()));
        }

        report.getBlockers().clear();
        if (request.getBlockers() != null) {
            request.getBlockers().forEach(dto -> report.getBlockers().add(
                    Blocker.builder()
                            .report(report)
                            .description(dto.getDescription())
                            .keyIssue(dto.isKeyIssue())
                            .build()));
        }

        report.getAchievements().clear();
        if (request.getAchievements() != null) {
            request.getAchievements().forEach(dto -> report.getAchievements().add(
                    Achievement.builder()
                            .report(report)
                            .description(dto.getDescription())
                            .keyAchievement(dto.isKeyAchievement())
                            .build()));
        }

        report.getTimeLogs().clear();
        if (request.getTimeLogs() != null) {
            request.getTimeLogs().forEach(dto -> report.getTimeLogs().add(
                    TimeLog.builder()
                            .report(report)
                            .taskType(dto.getTaskType())
                            .hours(dto.getHours())
                            .build()));
        }

        report.getNoteLinks().clear();
        if (request.getNoteLinks() != null) {
            request.getNoteLinks().forEach(dto -> report.getNoteLinks().add(
                    ReportNoteLink.builder()
                            .report(report)
                            .type(dto.getType())
                            .content(dto.getContent())
                            .build()));
        }
    }

    @SneakyThrows
    private void snapshotVersion(WeeklyReport report) {
        reportVersionRepository.findByReport_ReportIdAndCurrentTrue(report.getReportId())
                .ifPresent(previous -> {
                    previous.setCurrent(false);
                    reportVersionRepository.save(previous);
                });

        int nextVersionNumber = reportVersionRepository
                .findByReport_ReportIdOrderByVersionNumberAsc(report.getReportId())
                .size() + 1;

        Map<String, Object> snapshot = Map.of(
                "weekStartDate", report.getWeekStartDate(),
                "weekEndDate", report.getWeekEndDate(),
                "notes", report.getNotes() == null ? "" : report.getNotes(),
                "tasks", mapTasks(report),
                "nextWeekTasks", mapNextWeekTasks(report),
                "blockers", mapBlockers(report),
                "achievements", mapAchievements(report),
                "timeLogs", mapTimeLogs(report),
                "noteLinks", mapNoteLinks(report)
        );

        String snapshotJson = objectMapper.writeValueAsString(snapshot);

        ReportVersion version = ReportVersion.builder()
                .report(report)
                .versionNumber(nextVersionNumber)
                .contentSnapshot(snapshotJson)
                .submittedAt(LocalDateTime.now())
                .current(true)
                .build();

        reportVersionRepository.save(version);
    }

    private ReportResponseDTO toResponse(WeeklyReport report) {
        ReportVersion currentVersion = reportVersionRepository
                .findByReport_ReportIdAndCurrentTrue(report.getReportId())
                .orElse(null);

        return new ReportResponseDTO(
                report,
                mapTasks(report),
                mapNextWeekTasks(report),
                mapBlockers(report),
                mapAchievements(report),
                mapTimeLogs(report),
                mapNoteLinks(report),
                currentVersion != null ? currentVersion.getCommentPostedAt() : null,
                currentVersion != null && currentVersion.isCommentEdited());
    }

    private List<ReportTaskDTO> mapTasks(WeeklyReport report) {
        return report.getTasks().stream().map(t -> {
            ReportTaskDTO dto = new ReportTaskDTO();
            dto.setTaskName(t.getTaskName());
            dto.setPriority(t.getPriority());
            dto.setPlannedPct(t.getPlannedPct());
            dto.setActualPct(t.getActualPct());
            dto.setStatus(t.getStatus());
            dto.setTimePlanned(t.getTimePlanned());
            dto.setTimeSpent(t.getTimeSpent());
            dto.setDeliverable(t.getDeliverable());
            return dto;
        }).toList();
    }

    private List<NextWeekTaskDTO> mapNextWeekTasks(WeeklyReport report) {
        return report.getNextWeekTasks().stream().map(t -> {
            NextWeekTaskDTO dto = new NextWeekTaskDTO();
            dto.setTaskDescription(t.getTaskDescription());
            dto.setPriority(t.getPriority());
            return dto;
        }).toList();
    }

    private List<BlockerDTO> mapBlockers(WeeklyReport report) {
        return report.getBlockers().stream().map(b -> {
            BlockerDTO dto = new BlockerDTO();
            dto.setDescription(b.getDescription());
            dto.setKeyIssue(b.isKeyIssue());
            return dto;
        }).toList();
    }

    private List<AchievementDTO> mapAchievements(WeeklyReport report) {
        return report.getAchievements().stream().map(a -> {
            AchievementDTO dto = new AchievementDTO();
            dto.setDescription(a.getDescription());
            dto.setKeyAchievement(a.isKeyAchievement());
            return dto;
        }).toList();
    }

    private List<TimeLogDTO> mapTimeLogs(WeeklyReport report) {
        return report.getTimeLogs().stream().map(t -> {
            TimeLogDTO dto = new TimeLogDTO();
            dto.setTaskType(t.getTaskType());
            dto.setHours(t.getHours());
            return dto;
        }).toList();
    }

    private List<NoteLinkDTO> mapNoteLinks(WeeklyReport report) {
        return report.getNoteLinks().stream().map(n -> {
            NoteLinkDTO dto = new NoteLinkDTO();
            dto.setType(n.getType());
            dto.setContent(n.getContent());
            return dto;
        }).toList();
    }
}
