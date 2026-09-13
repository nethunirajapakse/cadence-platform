package com.cadence.dto;

import com.cadence.entity.WeeklyReport;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class ReportResponse {

    private final UUID reportId;
    private final UUID userId;
    private final String userName;
    private final UUID projectId;
    private final String projectName;
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    private final String status;
    private final String managerComment;
    private final String notes;
    private final LocalDateTime submittedAt;
    private final LocalDateTime approvedAt;
    private final List<ReportTaskDto> tasks;
    private final List<NextWeekTaskDto> nextWeekTasks;
    private final List<BlockerDto> blockers;
    private final List<AchievementDto> achievements;
    private final List<TimeLogDto> timeLogs;
    private final List<NoteLinkDto> noteLinks;
    // When the current manager comment was first posted - the frontend uses
    // this plus "now" to decide whether the 15-minute edit window is still open.
    private final LocalDateTime managerCommentPostedAt;
    // True once that comment has been corrected at least once - drives the
    // "(edited)" label.
    private final boolean managerCommentEdited;

    public ReportResponse(
            WeeklyReport report,
            List<ReportTaskDto> tasks,
            List<NextWeekTaskDto> nextWeekTasks,
            List<BlockerDto> blockers,
            List<AchievementDto> achievements,
            List<TimeLogDto> timeLogs,
            List<NoteLinkDto> noteLinks,
            LocalDateTime managerCommentPostedAt,
            boolean managerCommentEdited) {
        this.reportId = report.getReportId();
        this.userId = report.getUser().getUserId();
        this.userName = report.getUser().getName();
        this.projectId = report.getProject().getProjectId();
        this.projectName = report.getProject().getName();
        this.weekStartDate = report.getWeekStartDate();
        this.weekEndDate = report.getWeekEndDate();
        this.status = report.getStatus().name();
        this.managerComment = report.getManagerComment();
        this.notes = report.getNotes();
        this.submittedAt = report.getSubmittedAt();
        this.approvedAt = report.getApprovedAt();
        this.tasks = tasks;
        this.nextWeekTasks = nextWeekTasks;
        this.blockers = blockers;
        this.achievements = achievements;
        this.timeLogs = timeLogs;
        this.noteLinks = noteLinks;
        this.managerCommentPostedAt = managerCommentPostedAt;
        this.managerCommentEdited = managerCommentEdited;
    }
}
