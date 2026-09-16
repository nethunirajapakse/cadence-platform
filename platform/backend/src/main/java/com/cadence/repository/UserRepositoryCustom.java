package com.cadence.repository;

import com.cadence.dto.user.TeamMemberFilterCriteriaDTO;
import com.cadence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> findTeamMembersByFilters(TeamMemberFilterCriteriaDTO criteria, Pageable pageable);
}
