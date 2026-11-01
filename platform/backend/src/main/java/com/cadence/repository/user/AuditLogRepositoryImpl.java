package com.cadence.repository.user;

import com.cadence.entity.AuditLog;
import com.cadence.entity.QAuditLog;
import com.cadence.entity.enums.AuditAction;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<AuditLog> search(AuditAction action, String actorEmailContains, Pageable pageable) {
        QAuditLog audit = QAuditLog.auditLog;
        BooleanBuilder predicate = new BooleanBuilder();

        if (action != null) {
            predicate.and(audit.action.eq(action));
        }
        if (StringUtils.hasText(actorEmailContains)) {
            predicate.and(audit.actorEmail.containsIgnoreCase(actorEmailContains.trim()));
        }

        List<AuditLog> content = queryFactory
                .selectFrom(audit)
                .where(predicate)
                .orderBy(audit.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(audit.count()).from(audit).where(predicate).fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
