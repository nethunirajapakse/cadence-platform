package com.cadence.entity;

import com.cadence.entity.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "next_week_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextWeekTask {

    @Id
    @GeneratedValue
    @Column(name = "next_task_id")
    private UUID nextTaskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Column(name = "task_description", nullable = false, length = 300)
    private String taskDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private Priority priority;

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
