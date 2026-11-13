package com.needitbe.domain.campaign.repository;

import com.needitbe.domain.campaign.entity.CampaignBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignBannerRepository
        extends JpaRepository<CampaignBanner, Long> {

    // 특정 캠페인에 연결된 배너 조회
    List<CampaignBanner> findByCampaign_Id(Long campaignId);

    // 동일한 캠페인·배너·노출 영역 중복 확인
    boolean existsByCampaign_IdAndBanner_IdAndPlacement(
            Long campaignId,
            Long bannerId,
            String placement
    );

    // 특정 배너가 연결된 모든 캠페인 조회
    List<CampaignBanner> findByBanner_Id(Long bannerId);
    List<CampaignBanner> findByPlacement(String placement);

}