package com.needitbe.domain.ads.repository;

import com.needitbe.domain.ads.entity.AdExclusionReasonType;
import com.needitbe.domain.ads.entity.AdFilterExclusionReason;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdFilterExclusionReasonRepository
        extends JpaRepository<AdFilterExclusionReason, Long> {

    @Query("""
        SELECT r.reason, COUNT(r) FROM AdFilterExclusionReason r
        WHERE (:campaignId IS NULL OR r.evaluation.campaignId = :campaignId)
          AND (:start IS NULL OR r.evaluation.createdAt >= :start)
        GROUP BY r.reason ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countReasons(@Param("campaignId") Long campaignId, @Param("start") LocalDateTime start);

    // 캠페인별 제외 사유 집계
    @Query("""
        SELECT r.reason, COUNT(r)
        FROM AdFilterExclusionReason r
        WHERE r.evaluation.campaignId = :campaignId
        GROUP BY r.reason
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countReasonsByCampaign(
            @Param("campaignId") Long campaignId
    );

    // 캠페인별 특정 기간 제외 사유 집계
    @Query("""
        SELECT r.reason, COUNT(r)
        FROM AdFilterExclusionReason r
        WHERE r.evaluation.campaignId = :campaignId
          AND r.evaluation.createdAt >= :start
        GROUP BY r.reason
        ORDER BY COUNT(r) DESC
        """)
    List<Object[]> countReasonsByCampaignAndPeriod(
            @Param("campaignId") Long campaignId,
            @Param("start") LocalDateTime start
    );

    // 특정 제외 사유 건수
    long countByEvaluationCampaignIdAndReason(
            Long campaignId,
            AdExclusionReasonType reason
    );
}