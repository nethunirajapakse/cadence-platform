package com.cadence.repository.user;

import com.cadence.dto.user.TeamMemberFilterCriteria;
import com.cadence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.UUID;

public interface UserRepositoryCustom {
    // allowedUserIds is the caller's own scope (a manager's direct reports),
    // supplied by the service layer - never taken from request parameters.
    Page<User> findTeamMembersByFilters(TeamMemberFilterCriteria criteria, Collection<UUID> allowedUserIds, Pageable pageable);
}
