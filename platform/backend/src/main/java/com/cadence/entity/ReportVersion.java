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
