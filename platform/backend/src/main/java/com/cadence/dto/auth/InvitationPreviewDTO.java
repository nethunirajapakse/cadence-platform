package com.cadence.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// What the "accept invitation" page may show before the invitee has proven
// anything beyond holding the link: the address it was sent to and the role.
@Getter
@AllArgsConstructor
public class InvitationPreviewDTO {
    private String email;
    private String role;
    private String managerName;
    private LocalDateTime expiresAt;
}
