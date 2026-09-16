package com.cadence.dto.user;

import lombok.Getter;
import lombok.Setter;

// Bound from GET /api/dashboard/team-overview query params via Spring MVC's
// implicit @ModelAttribute binding - same pattern as ReportFilterCriteria and
// ProjectFilterCriteria.
@Getter
@Setter
public class TeamMemberFilterCriteria {
    private String name;
    private String email;
}
