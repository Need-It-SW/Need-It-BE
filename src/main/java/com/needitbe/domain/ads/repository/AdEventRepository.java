package com.needitbe.domain.ads.repository;

import com.needitbe.domain.ads.entity.AdEvent;
import com.needitbe.domain.ads.entity.AdEventType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


}