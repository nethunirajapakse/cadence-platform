package com.cadence.dto;

import com.cadence.entity.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Bound directly from the manager dashboard's query params via Spring MVC's
// implicit @ModelAttribute handling on plain object controller parameters -
// keeps the controller/service/repository signatures from growing an
// ever-longer list of individual filter parameters as filters get added.
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
