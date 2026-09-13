package com.cadence.repository;

import com.cadence.entity.ReportNoteLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportNoteLinkRepository extends JpaRepository<ReportNoteLink, UUID> {
    List<ReportNoteLink> findByReport_ReportId(UUID reportId);
}
