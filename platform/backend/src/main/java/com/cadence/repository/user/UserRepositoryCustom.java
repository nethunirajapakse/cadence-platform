package com.cadence.repository.user;

import com.cadence.dto.user.TeamMemberFilterCriteria;
import com.cadence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> findTeamMembersByFilters(TeamMemberFilterCriteria criteria, Pageable pageable);
}
