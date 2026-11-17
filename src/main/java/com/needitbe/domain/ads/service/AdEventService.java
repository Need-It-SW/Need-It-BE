package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdEventRequest;
import com.needitbe.domain.ads.entity.AdEvent;
import com.needitbe.domain.ads.entity.AdEventType;
import com.needitbe.domain.ads.repository.AdEventRepository;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;
import com.needitbe.domain.banner.repository.AdBannerRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdEventService {

    private final AdImpressionLimitService adImpressionLimitService;
    private final AdEventRepository adEventRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final AdBannerRepository adBannerRepository;
    private final CampaignBannerRepository campaignBannerRepository;

    // 광고 노출 기록
    @Transactional
    public void recordImpression(
            Long memberId,
            AdEventRequest request
    ) {

        // 1. 캠페인 잠금 획득
        var campaign = adCampaignRepository
                .findByIdForUpdate(request.campaignId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND)
                );

        // 2. 기존 노출 기록 확인
        AdEvent existing = adEventRepository
                .findByImpressionIdAndEventType(
                        request.impressionId(),
                        AdEventType.IMPRESSION
                )
                .orElse(null);

        // 3. 중복 노출 방지
        if (existing != null) {

            validateSameAd(existing, request);

            if (!java.util.Objects.equals(
                    existing.getMemberId(),
                    memberId
            )) {
                throw new CustomException(
                        ErrorCode.AD_EVENT_MISMATCH
                );
            }

            return;
        }

        // 4. 광고 유효성 검증
        validateAd(request);

        // 5. 최대 노출 횟수 확인
        // 5. 노출 한도 확인
        if (!adImpressionLimitService.canImpress(
                campaign,
                memberId
        )) {
            throw new CustomException(
                    ErrorCode.AD_IMPRESSION_LIMIT_EXCEEDED
            );
        }


        // 6. 노출 이벤트 생성
        AdEvent event = new AdEvent(
                request.campaignId(),
                request.bannerId(),
                memberId,
                AdEventType.IMPRESSION,
                request.impressionId()
        );

        // 7. DB 저장
        adEventRepository.save(event);
    }

    // 광고 클릭 기록
    @Transactional
    public void recordClick(AdEventRequest request) {

        // 1. 노출 기록 조회
        AdEvent impression = adEventRepository
                .findByImpressionIdAndEventType(
                        request.impressionId(),
                        AdEventType.IMPRESSION
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.AD_IMPRESSION_NOT_FOUND
                        )
                );

        // 2. 노출 기록과 광고 정보 일치 확인
        validateSameAd(impression, request);

        // 3. 기존 클릭 기록 확인
        AdEvent existingClick = adEventRepository
                .findByImpressionIdAndEventType(
                        request.impressionId(),
                        AdEventType.CLICK
                )
                .orElse(null);

        // 4. 중복 클릭 방지
        if (existingClick != null) {

            validateSameAd(existingClick, request);

            return;
        }

        // 5. 광고 유효성 검증
        validateAd(request);

        // 6. 클릭 이벤트 생성
        AdEvent event = new AdEvent(
                request.campaignId(),
                request.bannerId(),
                impression.getMemberId(),
                AdEventType.CLICK,
                request.impressionId()
        );

        // 7. DB 저장
        adEventRepository.save(event);
    }

    // 동일 광고인지 확인
    private void validateSameAd(
            AdEvent event,
            AdEventRequest request
    ) {

        if (!event.getCampaignId().equals(request.campaignId())
                || !event.getBannerId().equals(request.bannerId())) {

            throw new CustomException(
                    ErrorCode.AD_EVENT_MISMATCH
            );
        }
    }

    // 광고 유효성 검증
    private void validateAd(AdEventRequest request) {

        // 1. 캠페인 조회
        var campaign = adCampaignRepository.findById(request.campaignId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND)
                );

        // 2. 배너 조회
        var banner = adBannerRepository.findById(request.bannerId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.BANNER_NOT_FOUND)
                );

        // 3. 캠페인과 배너 연결 여부
        boolean connected =
                campaignBannerRepository.existsByCampaign_IdAndBanner_Id(
                        request.campaignId(),
                        request.bannerId()
                );

        if (!connected) {
            throw new CustomException(
                    ErrorCode.CAMPAIGN_BANNER_NOT_FOUND
            );
        }

        // 4. 진행 중인 캠페인인지 확인
        if (campaign.getStatus() != CampaignStatus.RUNNING) {
            throw new CustomException(ErrorCode.AD_NOT_ACTIVE);
        }

        // 5. 활성화된 배너인지 확인
        if (!banner.isActive()) {
            throw new CustomException(ErrorCode.AD_NOT_ACTIVE);
        }
    }


}