package com.cadence.repository;

import com.cadence.entity.QWeeklyReport;
import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class WeeklyReportRepositoryImpl implements WeeklyReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<WeeklyReport> findForDashboard(
            UUID userId, UUID projectId, ReportStatus status,
            LocalDate weekStart, LocalDate weekEnd, Pageable pageable) {

        QWeeklyReport report = QWeeklyReport.weeklyReport;

        BooleanBuilder predicate = new BooleanBuilder();

        // Drafts are never visible to a manager, per the spec: "only visible
        // to [the team member]". This is unconditional - it applies even if
        // a caller explicitly passes ?status=DRAFT, not just when status is
        // left unfiltered. Enforced here at the query level rather than only
        // in the frontend, so it can't be bypassed by calling the API directly.
        predicate.and(report.status.ne(ReportStatus.DRAFT));

        if (userId != null) {
            predicate.and(report.user.userId.eq(userId));
        }
        if (projectId != null) {
            predicate.and(report.project.projectId.eq(projectId));
        }
        if (status != null) {
            predicate.and(report.status.eq(status));
        }
        if (weekStart != null) {
            predicate.and(report.weekStartDate.goe(weekStart));
        }
        if (weekEnd != null) {
            predicate.and(report.weekEndDate.loe(weekEnd));
        }

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
