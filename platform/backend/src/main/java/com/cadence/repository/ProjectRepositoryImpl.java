package com.cadence.repository;

import com.cadence.dto.ProjectFilterCriteria;
import com.cadence.entity.Project;
import com.cadence.entity.QProject;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Project> findByFilters(ProjectFilterCriteria criteria) {
        QProject project = QProject.project;
        BooleanBuilder predicate = new BooleanBuilder();

        // Predicates are only added when a value is actually present - unlike
        // the earlier "? IS NULL OR ..." JPQL version, there's no bare
        // parameter ever sent to Postgres without a concrete column context,
        // so the type-inference issue that caused the bytea error can't occur.
        if (StringUtils.hasText(criteria.getName())) {
            predicate.and(project.name.containsIgnoreCase(criteria.getName()));
        }
        if (StringUtils.hasText(criteria.getDescription())) {
            predicate.and(project.description.containsIgnoreCase(criteria.getDescription()));
        }

        return queryFactory
                .selectFrom(project)
                .where(predicate)
                .orderBy(project.name.asc())
                .fetch();
    }
}
