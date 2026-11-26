package com.needitbe.domain.ads.repository;

import com.needitbe.domain.ads.entity.AdFilterEvaluation;
import com.needitbe.domain.ads.entity.AdFilterResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.needitbe.domain.ads.entity.AdExclusionReasonType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdFilterEvaluationRepository
        extends JpaRepository<AdFilterEvaluation, Long> {

    @Query("SELECT e FROM AdFilterEvaluation e WHERE NOT EXISTS (SELECT m.id FROM Member m WHERE m.id = e.memberId AND m.role = com.needitbe.domain.member.entity.MemberRole.ADMIN) AND (:campaignId IS NULL OR e.campaignId = :campaignId) AND (:result IS NULL OR e.result = :result) AND (:start IS NULL OR e.createdAt >= :start) ORDER BY e.createdAt DESC, e.id DESC")
    Page<AdFilterEvaluation> findRecentEvaluations(@Param("campaignId") Long campaignId, @Param("result") AdFilterResult result, @Param("start") LocalDateTime start, Pageable pageable);

    @Query("SELECT COUNT(e) FROM AdFilterEvaluation e WHERE NOT EXISTS (SELECT m.id FROM Member m WHERE m.id = e.memberId AND m.role = com.needitbe.domain.member.entity.MemberRole.ADMIN) AND (:campaignId IS NULL OR e.campaignId = :campaignId) AND (:start IS NULL OR e.createdAt >= :start)")
    long countEvaluations(@Param("campaignId") Long campaignId, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(e) FROM AdFilterEvaluation e WHERE NOT EXISTS (SELECT m.id FROM Member m WHERE m.id = e.memberId AND m.role = com.needitbe.domain.member.entity.MemberRole.ADMIN) AND (:campaignId IS NULL OR e.campaignId = :campaignId) AND e.result = :result AND (:start IS NULL OR e.createdAt >= :start)")
    long countEvaluationsByResult(@Param("campaignId") Long campaignId, @Param("result") AdFilterResult result, @Param("start") LocalDateTime start);

    // 캠페인별 전체 평가 건수
    long countByCampaignId(Long campaignId);

    // 캠페인별 선정 / 제외 건수
    long countByCampaignIdAndResult(
            Long campaignId,
            AdFilterResult result
    );

    // 특정 기간 평가 건수
    long countByCampaignIdAndCreatedAtGreaterThanEqual(
            Long campaignId,
            LocalDateTime start
    );

    // 특정 기간 선정 / 제외 건수
    long countByCampaignIdAndResultAndCreatedAtGreaterThanEqual(
            Long campaignId,
            AdFilterResult result,
            LocalDateTime start
    );

    // 캠페인별 평가 목록
    Page<AdFilterEvaluation> findByCampaignIdOrderByCreatedAtDesc(
            Long campaignId,
            Pageable pageable
    );

    @Query("""
    SELECT e
    FROM AdFilterEvaluation e
    WHERE NOT EXISTS (SELECT m.id FROM Member m WHERE m.id = e.memberId AND m.role = com.needitbe.domain.member.entity.MemberRole.ADMIN) AND (:campaignId IS NULL OR e.campaignId = :campaignId)
      AND e.result = com.needitbe.domain.ads.entity.AdFilterResult.EXCLUDED
      AND (:start IS NULL OR e.createdAt >= :start)
      AND (
          :reason IS NULL
          OR EXISTS (
              SELECT r.id
              FROM AdFilterExclusionReason r
              WHERE r.evaluation = e
                AND r.reason = :reason
          )
      )
    ORDER BY e.createdAt DESC, e.id DESC
    """)
    Page<AdFilterEvaluation> findExclusions(

            @Param("campaignId") Long campaignId,

            @Param("start") LocalDateTime start,

            @Param("reason") AdExclusionReasonType reason,

            Pageable pageable

    );
}