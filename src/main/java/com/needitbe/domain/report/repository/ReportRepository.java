package com.needitbe.domain.report.repository;

import com.needitbe.domain.report.entity.Report;
import com.needitbe.domain.report.entity.ReportStatus;
import com.needitbe.domain.report.entity.ReportType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 1. 내가 신고한 내역 조회
    List<Report> findByReporterIdOrderByCreatedAtDesc(
            Long reporterId
    );

    // 2. 동일 대상 중복 신고 방지 (접수 및 검토 중)
    boolean existsByReporterIdAndReportTypeAndTargetIdAndStatusIn(
            Long reporterId,
            ReportType reportType,
            Long targetId,
            List<ReportStatus> statuses
    );

    // 3. 관리자 - 신고 상태별 조회
    Page<Report> findByStatus(
            ReportStatus status,
            Pageable pageable
    );
}