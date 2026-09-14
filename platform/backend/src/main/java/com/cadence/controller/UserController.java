package com.cadence.controller;

import com.cadence.dto.TeamMemberSummary;
import com.cadence.entity.enums.RoleName;
import com.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Backs the manager dashboard's "filter by team member" dropdown - just
    // enough to populate a select, not a general-purpose user listing endpoint.
    @GetMapping("/team-members")
    @PreAuthorize("hasRole('MANAGER')")
    public List<TeamMemberSummary> getTeamMembers() {
        return userRepository.findByRoleName(RoleName.TEAM_MEMBER).stream()
                .map(TeamMemberSummary::new)
                .sorted(Comparator.comparing(TeamMemberSummary::getName))
                .toList();
    }
}
