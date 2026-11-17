package com.needitbe.domain.campaign.service;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.service.UserInterestService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedAdService {

    private final AdCampaignRepository adCampaignRepository;
    private final UserInterestService userInterestService;

    // =====================================================
    // 회원별 개인화 광고 추천
    // =====================================================
    public Optional<AdCampaign> recommendCampaign(Long memberId) {

        // 1. 회원 관심 카테고리 조회
        Map<RequestCategory, Long> interestScores =
                userInterestService.getCategoryInterestScores(memberId);

        // 2. 운영 중인 개인화 광고 조회
        List<AdCampaign> personalizedCampaigns =
                adCampaignRepository.findRunningCampaignsByAdType(
                        AdType.PERSONALIZED
                );

        // 3. 관심도가 높은 카테고리부터 광고 매칭
        for (RequestCategory category : interestScores.keySet()) {

            for (AdCampaign campaign : personalizedCampaigns) {

                if (campaign.getStatus() != CampaignStatus.RUNNING) {
                    continue;
                }

                if (matchesCategory(
                        campaign.getTargetCategory(),
                        category
                )) {
                    return Optional.of(campaign);
                }
            }
        }

        // 4. 개인화 광고가 없으면 일반 광고 조회
        List<AdCampaign> generalCampaigns =
                adCampaignRepository.findRunningCampaignsByAdType(
                        AdType.GENERAL
                );

        return generalCampaigns.stream()
                .filter(campaign ->
                        campaign.getStatus() == CampaignStatus.RUNNING
                )
                .findFirst();
    }

    // =====================================================
    // 광고 타겟 카테고리 비교
    // =====================================================
    private boolean matchesCategory(
            String targetCategory,
            RequestCategory category
    ) {

        if (targetCategory == null || targetCategory.isBlank()) {
            return false;
        }

        return targetCategory.trim()
                .equalsIgnoreCase(category.name());
    }
}