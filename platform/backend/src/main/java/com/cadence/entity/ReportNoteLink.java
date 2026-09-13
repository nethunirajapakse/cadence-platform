package com.cadence.entity;

import com.cadence.entity.enums.NoteLinkType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_notes_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportNoteLink {

    @Id
    @GeneratedValue
    @Column(name = "note_link_id")
    private UUID noteLinkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private NoteLinkType type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
