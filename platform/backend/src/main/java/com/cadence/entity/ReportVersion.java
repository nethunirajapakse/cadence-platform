package com.cadence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

// Snapshot of a report's full content, taken on every submit/resubmit.
// contentSnapshot is a JSON blob - deliberately not a diff, per the spec's
// "a simple list of past versions ... is sufficient".
@Entity
@Table(name = "report_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportVersion {

    @Id
    @GeneratedValue
    @Column(name = "version_id")
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_snapshot", nullable = false, columnDefinition = "jsonb")
    private String contentSnapshot;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // The review comment made against THIS version - null until the manager acts on it.
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // When the comment was FIRST posted - the anchor for the 15-minute edit
    // window. Never reset by an edit, only set the first time a comment is
    // written for this version.
    @Column(name = "comment_posted_at")
    private LocalDateTime commentPostedAt;

    // True once the comment has been corrected at least once - drives the
    // "(edited)" label in the UI.
    @Column(name = "comment_edited", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean commentEdited = false;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = true;

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
