package com.cadence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "Decision is required")
    private ReviewDecision decision;

    // Required only for REQUEST_CHANGES - checked in the service, not here,
    // since the requirement is conditional on the decision.
    @Size(max = 1000, message = "Comment must be 1000 characters or fewer")
    private String comment;
}
