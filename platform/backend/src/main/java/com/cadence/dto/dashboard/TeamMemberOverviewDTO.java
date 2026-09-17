package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TeamMemberOverviewDTO {
    private UUID userId;
    private String name;
    private String email;
    private String role;
    private int totalReports;
    private int approvedCount;
    private int needsCorrectionCount;
}
