package com.cadence.entity;

import com.cadence.entity.enums.Priority;
import com.cadence.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "report_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTask {

    @Id
    @GeneratedValue
    @Column(name = "task_id")
    private UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority;

    @Column(name = "planned_pct")
    private Integer plannedPct;

    @Column(name = "actual_pct")
    private Integer actualPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "time_planned", precision = 5, scale = 2)
    private BigDecimal timePlanned;

    @Column(name = "time_spent", precision = 5, scale = 2)
    private BigDecimal timeSpent;

    @Column(name = "deliverable", length = 300)
    private String deliverable;
}
