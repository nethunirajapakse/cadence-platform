package com.cadence.controller;

import com.cadence.dto.user.TeamMemberSummaryDTO;
import com.cadence.dto.user.UserProfileResponseDTO;
import com.cadence.entity.User;
import com.cadence.repository.user.UserRepository;
import com.cadence.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // A manager sees only their own direct reports.
    @GetMapping("/team-members")
    @PreAuthorize("hasRole('MANAGER')")
    public List<TeamMemberSummaryDTO> getTeamMembers(@AuthenticationPrincipal UserPrincipal principal) {
        return userRepository.findDirectReports(principal.getUserId()).stream()
                .map(TeamMemberSummaryDTO::new)
                .sorted(Comparator.comparing(TeamMemberSummaryDTO::getName))
                .toList();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    public UserProfileResponseDTO getUserProfile(
            @PathVariable UUID userId, @AuthenticationPrincipal UserPrincipal principal) {
        // Someone else's team member is reported exactly like a user that doesn't exist.
        if (!userRepository.isDirectReportOf(userId, principal.getUserId())) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return new UserProfileResponseDTO(user);
    }
}
