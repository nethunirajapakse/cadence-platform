package com.cadence.controller;

import com.cadence.dto.TeamMemberSummary;
import com.cadence.dto.UserProfileResponse;
import com.cadence.entity.User;
import com.cadence.entity.enums.RoleName;
import com.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // Backs the manager dashboard's "filter by team member" dropdown.
    @GetMapping("/team-members")
    @PreAuthorize("hasRole('MANAGER')")
    public List<TeamMemberSummary> getTeamMembers() {
        return userRepository.findByRoleName(RoleName.TEAM_MEMBER).stream()
                .map(TeamMemberSummary::new)
                .sorted(Comparator.comparing(TeamMemberSummary::getName))
                .toList();
    }

    // Backs the team-member profile page header (name/email/role). Manager-only -
    // a team member has no reason to look up another user's basic info this way.
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    public UserProfileResponse getUserProfile(@PathVariable UUID userId) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return new UserProfileResponse(user);
    }
}
