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

    // 회원 신고는 targetId, 게시글 신고는 게시글 작성자 ID로 집계한다.
    // 삭제(soft delete)된 게시글도 작성자가 남아 있으면 신고 이력에 포함한다.
    @Query(
            value = """
        SELECT CASE WHEN r.reportType = :memberType THEN r.targetId ELSE ir.member.id END AS memberId,
               COUNT(r) AS reportCount,
               SUM(CASE WHEN r.status = :pending THEN 1L ELSE 0L END) AS pendingCount,
               SUM(CASE WHEN r.status = :inReview THEN 1L ELSE 0L END) AS inReviewCount,
               SUM(CASE WHEN r.status = :resolved THEN 1L ELSE 0L END) AS resolvedCount,
               SUM(CASE WHEN r.status = :rejected THEN 1L ELSE 0L END) AS rejectedCount
        FROM Report r
        LEFT JOIN ItemRequest ir ON r.reportType = :requestType AND ir.id = r.targetId
        LEFT JOIN ReportTargetDismissal d ON d.memberId = CASE WHEN r.reportType = :memberType THEN r.targetId ELSE ir.member.id END
        WHERE r.reportType = :memberType OR (r.reportType = :requestType AND ir.id IS NOT NULL)
        GROUP BY CASE WHEN r.reportType = :memberType THEN r.targetId ELSE ir.member.id END
        HAVING MAX(r.id) > COALESCE(MAX(d.lastReportId), 0L)
        ORDER BY COUNT(r) DESC, CASE WHEN r.reportType = :memberType THEN r.targetId ELSE ir.member.id END ASC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT CASE WHEN r.reportType = :memberType THEN r.targetId ELSE ir.member.id END)
        FROM Report r
        LEFT JOIN ItemRequest ir ON r.reportType = :requestType AND ir.id = r.targetId
        WHERE (r.reportType = :memberType OR (r.reportType = :requestType AND ir.id IS NOT NULL))
          AND r.id > COALESCE((SELECT MAX(d.lastReportId)
              FROM ReportTargetDismissal d
              WHERE d.memberId = CASE WHEN r.reportType = :memberType THEN r.targetId ELSE ir.member.id END), 0L)
        """
    )
    Page<ReportedMemberProjection> findReportedMembers(
            @Param("memberType") ReportType memberType,
            @Param("requestType") ReportType requestType,
            @Param("pending") ReportStatus pending,
            @Param("inReview") ReportStatus inReview,
            @Param("resolved") ReportStatus resolved,
            @Param("rejected") ReportStatus rejected,
            Pageable pageable
    );

    @Query("""
        SELECT MAX(r.id) FROM Report r
        LEFT JOIN ItemRequest ir ON r.reportType = :requestType AND ir.id = r.targetId
        WHERE (r.reportType = :memberType AND r.targetId = :memberId)
           OR (r.reportType = :requestType AND ir.member.id = :memberId)
        """)
    Long findLatestTargetReportId(@Param("memberId") Long memberId,
                                  @Param("memberType") ReportType memberType,
                                  @Param("requestType") ReportType requestType);

    void deleteByReportTypeAndTargetId(
            ReportType reportType,
            Long targetId
    );
}