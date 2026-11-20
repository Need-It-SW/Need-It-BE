package com.needitbe.domain.report.repository;

import com.needitbe.domain.report.dto.ReportedMemberProjection;
import com.needitbe.domain.report.entity.Report;
import com.needitbe.domain.report.entity.ReportStatus;
import com.needitbe.domain.report.entity.ReportType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(
            value = """
        SELECT r.targetId AS memberId,
               COUNT(r) AS reportCount,
               SUM(CASE WHEN r.status = :pending THEN 1 ELSE 0 END) AS pendingCount,
               SUM(CASE WHEN r.status = :inReview THEN 1 ELSE 0 END) AS inReviewCount,
               SUM(CASE WHEN r.status = :resolved THEN 1 ELSE 0 END) AS resolvedCount,
               SUM(CASE WHEN r.status = :rejected THEN 1 ELSE 0 END) AS rejectedCount
        FROM Report r
        WHERE r.reportType = :memberType
        GROUP BY r.targetId
        ORDER BY COUNT(r) DESC, r.targetId ASC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT r.targetId)
        FROM Report r
        WHERE r.reportType = :memberType
        """
    )
    Page<ReportedMemberProjection> findReportedMembers(
            @Param("memberType") ReportType memberType,
            @Param("pending") ReportStatus pending,
            @Param("inReview") ReportStatus inReview,
            @Param("resolved") ReportStatus resolved,
            @Param("rejected") ReportStatus rejected,
            Pageable pageable
    );
}