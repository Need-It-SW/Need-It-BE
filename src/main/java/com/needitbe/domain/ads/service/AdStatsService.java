package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdStatsResponse;
import com.needitbe.domain.ads.entity.AdEventType;
import com.needitbe.domain.ads.repository.AdEventRepository;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdStatsService {

    private final AdEventRepository adEventRepository;
    private final AdCampaignRepository adCampaignRepository;

    // 캠페인별 광고 통계 조회
    @Transactional(readOnly = true)
    public AdStatsResponse getCampaignStats(Long campaignId) {

        // 1. 캠페인 존재 여부 확인
        if (!adCampaignRepository.existsById(campaignId)) {
            throw new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND);
        }

        // 2. 광고 노출 횟수 조회
        long impressions =
                adEventRepository.countByCampaignIdAndEventType(
                        campaignId,
                        AdEventType.IMPRESSION
                );

        // 3. 광고 클릭 횟수 조회
        long clicks =
                adEventRepository.countByCampaignIdAndEventType(
                        campaignId,
                        AdEventType.CLICK
                );

        // 4. 클릭률 계산
        double ctr = 0.0;

        if (impressions > 0) {
            ctr = (double) clicks / impressions * 100;
        }

        // 5. 소수점 둘째 자리까지 반올림
        ctr = Math.round(ctr * 100.0) / 100.0;

        // 6. 응답 반환
        return new AdStatsResponse(
                campaignId,
                impressions,
                clicks,
                ctr
        );
    }
}