package com.needitbe.domain.campaign.service;

import com.needitbe.domain.campaign.dto.*;
import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.CampaignBanner;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdCampaignService {

    private final CampaignBannerRepository campaignBannerRepository;
    private final AdCampaignRepository adCampaignRepository;

    // 캠페인 등록
    @Transactional
    public AdCampaignResponse createCampaign(
            AdCampaignCreateRequest request
    ) {

        AdCampaign campaign = new AdCampaign(request);

        AdCampaign saved =
                adCampaignRepository.saveAndFlush(campaign);

        return AdCampaignResponse.from(saved);
    }

    // 캠페인 목록 조회 및 검색
    @Transactional(readOnly = true)
    public List<AdCampaignResponse> getCampaigns(
            String keyword,
            CampaignStatus status
    ) {

        return adCampaignRepository.findAll().stream()

                .filter(campaign ->
                        keyword == null || keyword.isBlank()
                                || campaign.getName()
                                .toLowerCase()
                                .contains(keyword.trim().toLowerCase())
                )

                .filter(campaign ->
                        status == null
                                || campaign.getStatus() == status
                )

                .map(AdCampaignResponse::from)
                .toList();
    }

    // 캠페인 상세 조회
    @Transactional(readOnly = true)
    public AdCampaignResponse getCampaign(Long campaignId) {

        AdCampaign campaign =
                adCampaignRepository.findById(campaignId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CAMPAIGN_NOT_FOUND
                                )
                        );

        return AdCampaignResponse.from(campaign);
    }

    // 캠페인 수정
    @Transactional
    public AdCampaignResponse updateCampaign(
            Long campaignId,
            AdCampaignUpdateRequest request
    ) {

        AdCampaign campaign =
                adCampaignRepository.findById(campaignId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.CAMPAIGN_NOT_FOUND
                                )
                        );

        campaign.update(request);

        adCampaignRepository.flush();

        return AdCampaignResponse.from(campaign);
    }

    // 캠페인 삭제
    // 관리자 광고 캠페인 삭제
    @Transactional
    public void deleteCampaign(Long campaignId) {

        // 1. 캠페인 조회
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND)
                );

        // 2. 캠페인에 연결된 배너 관계 조회
        List<CampaignBanner> campaignBanners =
                campaignBannerRepository.findByCampaign_Id(campaignId);

        // 3. 연결 정보 삭제
        campaignBannerRepository.deleteAll(campaignBanners);
        campaignBannerRepository.flush();

        // 4. 캠페인 삭제
        adCampaignRepository.delete(campaign);
        adCampaignRepository.flush();
    }
}