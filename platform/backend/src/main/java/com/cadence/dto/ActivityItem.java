package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ActivityItem {
    private UUID reportId;
    private String userName;
    private String projectName;
    private String status;
    private LocalDateTime actionAt;
    private String description;
}
