package com.cadence.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "Decision is required")
    private ReviewDecision decision;

    // Required only for REQUEST_CHANGES - checked in the service, not here,
    // since the requirement is conditional on the decision.
    private String comment;
}
