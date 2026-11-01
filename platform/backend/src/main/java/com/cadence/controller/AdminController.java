package com.cadence.controller;

import com.cadence.dto.admin.*;
import com.cadence.entity.enums.AuditAction;
import com.cadence.repository.user.AuditLogRepository;
import com.cadence.security.UserPrincipal;
import com.cadence.service.AdminUserService;
import com.cadence.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// User administration. Administrators manage accounts, invitations and can read
// the audit log, but they are NOT given access to report content.
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminUserService adminUserService;
    private final InvitationService invitationService;
    private final AuditLogRepository auditLogRepository;

    // ---- users ---------------------------------------------------------------

    @GetMapping("/users")
    public List<AdminUserResponseDTO> listUsers() {
        return adminUserService.listUsers();
    }

    @PatchMapping("/users/{userId}")
    public AdminUserResponseDTO updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal admin,
            HttpServletRequest http) {
        return adminUserService.update(admin, userId, request, http.getRemoteAddr());
    }

    @PostMapping("/users/{userId}/deactivate")
    public AdminUserResponseDTO deactivate(
            @PathVariable UUID userId, @AuthenticationPrincipal UserPrincipal admin, HttpServletRequest http) {
        return adminUserService.deactivate(admin, userId, http.getRemoteAddr());
    }

    @PostMapping("/users/{userId}/reactivate")
    public AdminUserResponseDTO reactivate(
            @PathVariable UUID userId, @AuthenticationPrincipal UserPrincipal admin, HttpServletRequest http) {
        return adminUserService.reactivate(admin, userId, http.getRemoteAddr());
    }

    // ---- invitations -----------------------------------------------------------

    @GetMapping("/invitations")
    public List<InvitationResponseDTO> listInvitations() {
        return invitationService.listAll();
    }

    @PostMapping("/invitations")
    public ResponseEntity<InvitationResponseDTO> invite(
            @Valid @RequestBody InvitationRequestDTO request,
            @AuthenticationPrincipal UserPrincipal admin,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.create(admin, request, http.getRemoteAddr()));
    }

    @PostMapping("/invitations/{invitationId}/resend")
    public InvitationResponseDTO resend(
            @PathVariable UUID invitationId, @AuthenticationPrincipal UserPrincipal admin, HttpServletRequest http) {
        return invitationService.resend(admin, invitationId, http.getRemoteAddr());
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID invitationId, @AuthenticationPrincipal UserPrincipal admin, HttpServletRequest http) {
        invitationService.revoke(admin, invitationId, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    // ---- audit log ---------------------------------------------------------------

    @GetMapping("/audit-log")
    public Page<AuditLogResponseDTO> auditLog(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return auditLogRepository.search(action, actor, pageable).map(AuditLogResponseDTO::new);
    }
}
