package com.needitbe.domain.ads.repository;

import com.needitbe.domain.ads.entity.AdEvent;
import com.needitbe.domain.ads.entity.AdEventType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.needitbe.domain.ads.entity.AdEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
    SELECT e
    FROM AdEvent e
    WHERE e.campaignId = :campaignId
      AND (:start IS NULL OR e.createdAt >= :start)
      AND (:eventType IS NULL OR e.eventType = :eventType)
    ORDER BY e.createdAt DESC, e.id DESC
    """)
    Page<AdEvent> findAdminAdEvents(
            @Param("campaignId") Long campaignId,
            @Param("start") LocalDateTime start,
            @Param("eventType") AdEventType eventType,
            Pageable pageable
    );
    @Query(value = """
        SELECT DATE(e.created_at), e.event_type, COUNT(*)
        FROM ad_events e JOIN ad_campaigns c ON c.id = e.campaign_id
        WHERE e.created_at >= :start AND e.created_at < :end
          AND (:campaignId IS NULL OR e.campaign_id = :campaignId)
          AND (:adType IS NULL OR c.ad_type = :adType)
        GROUP BY DATE(e.created_at), e.event_type
        ORDER BY DATE(e.created_at)
        """, nativeQuery = true)
    List<Object[]> dailyStatistics(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("campaignId") Long campaignId,
            @Param("adType") String adType);
}