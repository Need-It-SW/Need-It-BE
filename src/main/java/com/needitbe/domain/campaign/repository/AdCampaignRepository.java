package com.needitbe.domain.campaign.repository;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.AdType;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdCampaignRepository
        extends JpaRepository<AdCampaign, Long> {

    // 기존 캠페인 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AdCampaign c WHERE c.id = :campaignId")
    Optional<AdCampaign> findByIdForUpdate(
            @Param("campaignId") Long campaignId
    );

    // 현재 운영 가능한 광고 캠페인 조회
    @Query("""
            SELECT c
            FROM AdCampaign c
            WHERE c.active = true
              AND c.adType = :adType
              AND c.startDate <= CURRENT_DATE
              AND c.endDate >= CURRENT_DATE
            ORDER BY c.id DESC
            """)
    List<AdCampaign> findRunningCampaignsByAdType(
            @Param("adType") AdType adType
    );

    // 현재 진행 중인 전체 캠페인 조회
    @Query("""
    SELECT c
    FROM AdCampaign c
    WHERE c.active = true
      AND c.startDate <= CURRENT_DATE
      AND c.endDate >= CURRENT_DATE
    ORDER BY c.id DESC
    """)
    List<AdCampaign> findAllRunningCampaigns();
}