package com.needitbe.domain.campaign.service;

import com.needitbe.domain.banner.entity.AdBanner;
import com.needitbe.domain.banner.repository.AdBannerRepository;

import com.needitbe.domain.campaign.dto.CampaignBannerCreateRequest;
import com.needitbe.domain.campaign.dto.CampaignBannerResponse;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.CampaignBanner;

import com.needitbe.domain.campaign.repository.AdCampaignRepository;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;

import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignBannerService {

    private final CampaignBannerRepository campaignBannerRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final AdBannerRepository adBannerRepository;
    private final S3Service s3Service;

    // 캠페인에 배너 연결
    @Transactional
    public CampaignBannerResponse addBanner(
            Long campaignId,
            CampaignBannerCreateRequest request
    ) {

        AdCampaign campaign =
                adCampaignRepository.findById(campaignId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CAMPAIGN_NOT_FOUND
                                )
                        );

        AdBanner banner =
                adBannerRepository.findById(request.bannerId())
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.BANNER_NOT_FOUND
                                )
                        );

        String placement = request.placement().trim();

        boolean exists =
                campaignBannerRepository
                        .existsByCampaign_IdAndBanner_IdAndPlacement(
                                campaignId,
                                request.bannerId(),
                                placement
                        );

        if (exists) {
            throw new IllegalArgumentException(
                    "이미 해당 캠페인과 노출 영역에 연결된 배너입니다."
            );
        }

        CampaignBanner campaignBanner =
                new CampaignBanner(
                        campaign,
                        banner,
                        placement
                );

        CampaignBanner saved =
                campaignBannerRepository.saveAndFlush(campaignBanner);

        String imageUrl =
                s3Service.getPresignedUrl(
                        saved.getBanner().getImageKey()
                );

        return CampaignBannerResponse.from(saved, imageUrl);
    }

    // 캠페인에 연결된 배너 목록 조회
    @Transactional(readOnly = true)
    public List<CampaignBannerResponse> getCampaignBanners(
            Long campaignId
    ) {

        if (!adCampaignRepository.existsById(campaignId)) {
            throw new CustomException(
                    ErrorCode.CAMPAIGN_NOT_FOUND
            );
        }

        return campaignBannerRepository
                .findByCampaign_Id(campaignId)
                .stream()
                .map(campaignBanner -> {

                    String imageKey =
                            campaignBanner.getBanner().getImageKey();

                    String imageUrl =
                            s3Service.getPresignedUrl(imageKey);

                    return CampaignBannerResponse.from(
                            campaignBanner,
                            imageUrl
                    );
                })
                .toList();
    }

    // 캠페인 배너 연결 해제
    @Transactional
    public void removeBanner(
            Long campaignId,
            Long campaignBannerId
    ) {

        if (!adCampaignRepository.existsById(campaignId)) {
            throw new CustomException(
                    ErrorCode.CAMPAIGN_NOT_FOUND
            );
        }

        CampaignBanner campaignBanner =
                campaignBannerRepository.findById(campaignBannerId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CAMPAIGN_BANNER_NOT_FOUND
                                )
                        );

        if (!campaignBanner.getCampaign()
                .getId().equals(campaignId)) {

            throw new CustomException(
                    ErrorCode.CAMPAIGN_BANNER_NOT_FOUND
            );
        }

        campaignBannerRepository.delete(campaignBanner);
    }
}