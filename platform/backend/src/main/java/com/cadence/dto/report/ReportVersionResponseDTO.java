package com.cadence.dto.report;

import com.cadence.entity.ReportVersion;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ReportVersionResponseDTO {
    private final UUID versionId;
    private final int versionNumber;
    private final LocalDateTime submittedAt;
    private final String comment;
    private final boolean current;

    public ReportVersionResponseDTO(ReportVersion version) {
        this.versionId = version.getVersionId();
        this.versionNumber = version.getVersionNumber();
        this.submittedAt = version.getSubmittedAt();
        this.comment = version.getComment();
        this.current = version.isCurrent();
    }
}
