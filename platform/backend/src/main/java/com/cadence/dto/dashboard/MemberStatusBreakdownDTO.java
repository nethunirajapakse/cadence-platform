package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberStatusBreakdownDTO {
    private String userName;
    private int submitted;
    private int needsCorrection;
    private int approved;
}
