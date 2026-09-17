package com.cadence.repository.project;

import com.cadence.dto.project.ProjectFilterCriteria;
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
