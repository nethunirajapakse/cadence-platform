package com.cadence.dto.project;

import lombok.Getter;
import lombok.Setter;

// Bound from GET /api/projects query params via Spring MVC's implicit
// @ModelAttribute handling - same pattern as ReportFilterCriteria for the
// reports dashboard. Both fields are optional; a blank/absent field means
// "don't filter on this," matching how the report filters behave.
@Getter
@Setter
public class ProjectFilterCriteria {
    private String name;
    private String description;
}
