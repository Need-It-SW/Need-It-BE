package com.needitbe.domain.ads.repository;

import com.needitbe.domain.ads.entity.AdEvent;
import com.needitbe.domain.ads.entity.AdEventType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdEventRepository
        extends JpaRepository<AdEvent, Long> {

    // 캠페인별 특정 이벤트 수 조회
    long countByCampaignIdAndEventType(
            Long campaignId,
            AdEventType eventType
    );

    // 배너별 특정 이벤트 수 조회
    long countByBannerIdAndEventType(
            Long bannerId,
            AdEventType eventType
    );

    boolean existsByImpressionIdAndEventType(
            String impressionId,
            AdEventType eventType
    );

    Optional<AdEvent> findByImpressionIdAndEventType(
            String impressionId,
            AdEventType eventType
    );

    @Query("""
    SELECT e.campaignId, COUNT(e)
    FROM AdEvent e
    WHERE e.eventType = :eventType
      AND e.campaignId IN :campaignIds
    GROUP BY e.campaignId
""")
    List<Object[]> countByCampaignIdsAndEventType(
            @Param("campaignIds") List<Long> campaignIds,
            @Param("eventType") AdEventType eventType
    );


    // 특정 기간 캠페인 노출 횟수

    long countByCampaignIdAndEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(

            Long campaignId,

            AdEventType eventType,

            LocalDateTime start,

            LocalDateTime end

    );

// 여러 캠페인의 특정 기간 노출 횟수

    @Query("""

    SELECT e.campaignId, COUNT(e)

    FROM AdEvent e

    WHERE e.eventType = :eventType

      AND e.campaignId IN :campaignIds

      AND e.createdAt >= :start

      AND e.createdAt < :end

    GROUP BY e.campaignId

""")

    List<Object[]> countByCampaignIdsAndEventTypeBetween(

            @Param("campaignIds") List<Long> campaignIds,

            @Param("eventType") AdEventType eventType,

            @Param("start") LocalDateTime start,

            @Param("end") LocalDateTime end

    );

    // 회원별 캠페인 전체 노출 횟수
    long countByCampaignIdAndMemberIdAndEventType(
            Long campaignId,
            Long memberId,
            AdEventType eventType
    );

    // 회원별 특정 기간 노출 횟수
    long countByCampaignIdAndMemberIdAndEventTypeAndCreatedAtGreaterThanEqual(
            Long campaignId,
            Long memberId,
            AdEventType eventType,
            LocalDateTime start
    );

}