package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;
import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdService {

    private final CampaignBannerRepository campaignBannerRepository;
    private final S3Service s3Service;

    // 사용자 광고 조회
    @Transactional(readOnly = true)
    public List<AdResponse> getAds(String placement) {

        return campaignBannerRepository.findByPlacement(placement)
                .stream()

                // 진행 중인 캠페인만 조회
                .filter(cb ->
                        cb.getCampaign().getStatus()
                                == CampaignStatus.RUNNING
                )

                // 일반 광고만 조회
                .filter(cb ->
                        cb.getCampaign().getAdType().name()
                                .equals("GENERAL")
                )

                // 활성화된 배너만 조회
                .filter(cb ->
                        cb.getBanner().isActive()
                )

                // 응답 변환
                .map(cb -> AdResponse.from(
                        cb,
                        s3Service.getPresignedUrl(
                                cb.getBanner().getImageKey()
                        )
                ))

                .toList();
    }
}