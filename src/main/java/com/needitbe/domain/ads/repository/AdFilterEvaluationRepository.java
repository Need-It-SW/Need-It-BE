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
    WHERE e.campaignId = :campaignId
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