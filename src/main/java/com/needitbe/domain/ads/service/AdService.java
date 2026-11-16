package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.ads.entity.AdEventType;
import com.needitbe.domain.ads.repository.AdEventRepository;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;
import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdService {

    private final AdEventRepository adEventRepository;
    private final CampaignBannerRepository campaignBannerRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public List<AdResponse> getAds(String placement) {

        // 1. 광고 배너 조회 및 기본 조건 필터링
        var campaignBanners = campaignBannerRepository
                .findByPlacement(placement)
                .stream()
                .filter(cb ->
                        cb.getCampaign().getStatus()
                                == CampaignStatus.RUNNING
                )
                .filter(cb ->
                        cb.getCampaign().getAdType()
                                .name().equals("GENERAL")
                )
                .filter(cb -> cb.getBanner().isActive())
                .toList();

        if (campaignBanners.isEmpty()) {
            return List.of();
        }

        // 2. 캠페인 ID 추출
        List<Long> campaignIds = campaignBanners.stream()
                .map(cb -> cb.getCampaign().getId())
                .distinct()
                .toList();

        // 3. 캠페인별 노출 횟수 한 번에 조회
        Map<Long, Long> impressionCounts =
                adEventRepository.countByCampaignIdsAndEventType(
                                campaignIds,
                                AdEventType.IMPRESSION
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        // 4. 최대 노출 횟수 확인 후 광고 반환
        return campaignBanners.stream()
                .filter(cb -> {

                    var campaign = cb.getCampaign();

                    Integer maxImpressions =
                            campaign.getMaxImpressions();

                    if (maxImpressions == null) {
                        return true;
                    }

                    long currentImpressions =
                            impressionCounts.getOrDefault(
                                    campaign.getId(),
                                    0L
                            );

                    return currentImpressions < maxImpressions;
                })
                .map(cb -> AdResponse.from(
                        cb,
                        s3Service.getPresignedUrl(
                                cb.getBanner().getImageKey()
                        ),
                        UUID.randomUUID().toString()
                ))
                .toList();
    }
}