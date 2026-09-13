package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberStatusBreakdown {
    private String userName;
    private int submitted;
    private int needsCorrection;
    private int approved;
}
