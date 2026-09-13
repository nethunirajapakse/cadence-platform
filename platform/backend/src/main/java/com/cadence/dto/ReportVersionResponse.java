package com.cadence.dto;

import com.cadence.entity.ReportVersion;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// Deliberately does NOT include contentSnapshot - a bare list of past versions
// with timestamps is what the spec asks for ("simple list ... is sufficient").
// The snapshot itself is available via a separate detail endpoint on demand.
@Getter
public class ReportVersionResponse {
    private final UUID versionId;
    private final int versionNumber;
    private final LocalDateTime submittedAt;
    private final String comment;
    private final boolean current;

    public ReportVersionResponse(ReportVersion version) {
        this.versionId = version.getVersionId();
        this.versionNumber = version.getVersionNumber();
        this.submittedAt = version.getSubmittedAt();
        this.comment = version.getComment();
        this.current = version.isCurrent();
    }
}
