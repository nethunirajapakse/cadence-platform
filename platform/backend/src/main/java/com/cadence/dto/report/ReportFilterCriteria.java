package com.cadence.dto.report;

import com.cadence.entity.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReportFilterCriteria {
    private UUID userId;
    private List<UUID> projectIds;
    private boolean excludeProjects;
    private List<ReportStatus> statuses;
    private LocalDate weekStart;
    private LocalDate weekEnd;
}
