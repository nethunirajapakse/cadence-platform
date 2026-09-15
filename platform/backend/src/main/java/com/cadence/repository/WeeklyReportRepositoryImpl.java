package com.cadence.repository;

import com.cadence.dto.ReportFilterCriteria;
import com.cadence.entity.QWeeklyReport;
import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class WeeklyReportRepositoryImpl implements WeeklyReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<WeeklyReport> findForDashboard(ReportFilterCriteria criteria, Pageable pageable) {
        QWeeklyReport report = QWeeklyReport.weeklyReport;

        BooleanBuilder predicate = new BooleanBuilder();

        // Drafts are never visible to a manager, per the spec - unconditional,
        // applies even if a caller explicitly passes statuses containing DRAFT.
        predicate.and(report.status.ne(ReportStatus.DRAFT));

        applyCommonFilters(predicate, report, criteria);

        return runQuery(report, predicate, pageable);
    }

    @Override
    public Page<WeeklyReport> findForOwnHistory(UUID userId, ReportFilterCriteria criteria, Pageable pageable) {
        QWeeklyReport report = QWeeklyReport.weeklyReport;

        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(report.user.userId.eq(userId)); // always scoped to the caller - no status exclusion here

        applyCommonFilters(predicate, report, criteria);

        return runQuery(report, predicate, pageable);
    }

    private void applyCommonFilters(BooleanBuilder predicate, QWeeklyReport report, ReportFilterCriteria criteria) {
        if (criteria.getUserId() != null) {
            predicate.and(report.user.userId.eq(criteria.getUserId()));
        }

        List<UUID> projectIds = criteria.getProjectIds();
        if (projectIds != null && !projectIds.isEmpty()) {
            predicate.and(criteria.isExcludeProjects()
                    ? report.project.projectId.notIn(projectIds)
                    : report.project.projectId.in(projectIds));
        }

        List<ReportStatus> statuses = criteria.getStatuses();
        if (statuses != null && !statuses.isEmpty()) {
            predicate.and(report.status.in(statuses));
        }

        if (criteria.getWeekStart() != null) {
            predicate.and(report.weekStartDate.goe(criteria.getWeekStart()));
        }
        if (criteria.getWeekEnd() != null) {
            predicate.and(report.weekEndDate.loe(criteria.getWeekEnd()));
        }
    }

    private Page<WeeklyReport> runQuery(QWeeklyReport report, BooleanBuilder predicate, Pageable pageable) {
        List<WeeklyReport> content = queryFactory
                .selectFrom(report)
                .where(predicate)
                .orderBy(report.weekStartDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(report.count())
                .from(report)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }
}
