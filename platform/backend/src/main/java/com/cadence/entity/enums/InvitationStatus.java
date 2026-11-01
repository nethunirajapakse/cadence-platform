package com.cadence.entity.enums;

// Stored statuses only. "Expired" is not stored - it is derived at read time
// from a PENDING invitation whose expiresAt has passed.
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED
}
