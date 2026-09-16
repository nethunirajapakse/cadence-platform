package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ActivityItemDTO {
    private UUID reportId;
    private String userName;
    private String projectName;
    private String status;
    private LocalDateTime actionAt;
    private String description;
}
