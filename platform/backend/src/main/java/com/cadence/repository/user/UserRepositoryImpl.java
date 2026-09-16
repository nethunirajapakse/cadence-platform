package com.cadence.repository.user;

import com.cadence.dto.user.TeamMemberFilterCriteria;
import com.cadence.entity.QUser;
import com.cadence.entity.User;
import com.cadence.entity.enums.RoleName;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<User> findTeamMembersByFilters(TeamMemberFilterCriteria criteria, Pageable pageable) {
        QUser user = QUser.user;
        BooleanBuilder predicate = new BooleanBuilder();

        predicate.and(user.role.roleName.eq(RoleName.TEAM_MEMBER));

        if (StringUtils.hasText(criteria.getName())) {
            predicate.and(user.name.containsIgnoreCase(criteria.getName()));
        }
        if (StringUtils.hasText(criteria.getEmail())) {
            predicate.and(user.email.containsIgnoreCase(criteria.getEmail()));
        }

        List<User> content = queryFactory
                .selectFrom(user)
                .where(predicate)
                .orderBy(user.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory.select(user.count()).from(user).where(predicate).fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
