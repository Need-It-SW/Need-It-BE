package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdStatisticsResponse;
import com.needitbe.domain.ads.entity.AdEventType;
import com.needitbe.domain.ads.repository.AdEventRepository;
import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.needitbe.domain.ads.dto.AdBannerStatisticsResponse;
import com.needitbe.domain.banner.entity.AdBanner;
import com.needitbe.domain.banner.repository.AdBannerRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdStatisticsService {

    private final AdBannerRepository adBannerRepository;
    private final AdEventRepository adEventRepository;
    private final AdCampaignRepository adCampaignRepository;

    // 캠페인별 광고 통계 조회
    public AdStatisticsResponse getCampaignStatistics(
            Long campaignId
    ) {

        // 1. 캠페인 조회
        AdCampaign campaign = adCampaignRepository
                .findById(campaignId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CAMPAIGN_NOT_FOUND
                        )
                );

        // 2. 노출 수 조회
        long impressions =
                adEventRepository.countByCampaignIdAndEventType(
                        campaignId,
                        AdEventType.IMPRESSION
                );

        // 3. 클릭 수 조회
        long clicks =
                adEventRepository.countByCampaignIdAndEventType(
                        campaignId,
                        AdEventType.CLICK
                );

        // 4. CTR 계산
        double ctr = 0.0;

        if (impressions > 0) {
            ctr = (double) clicks / impressions * 100;
        }

        // 소수점 둘째 자리까지 반올림
        ctr = Math.round(ctr * 100.0) / 100.0;

        // 5. 응답 반환
        return new AdStatisticsResponse(
                campaign.getId(),
                campaign.getName(),
                impressions,
                clicks,
                ctr,
                campaign.getAdType()
        );
    }

    // 전체 캠페인 광고 통계 조회
    public List<AdStatisticsResponse> getAllCampaignStatistics() {

        return adCampaignRepository.findAll()
                .stream()
                .map(campaign ->
                        getCampaignStatistics(campaign.getId())
                )
                .toList();
    }


    // 특정 기간 캠페인 광고 통계 조회
    public AdStatisticsResponse getCampaignStatisticsByPeriod(
            Long campaignId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "조회 시작일은 종료일보다 이전이어야 합니다."
            );
        }

        AdCampaign campaign = adCampaignRepository
                .findById(campaignId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CAMPAIGN_NOT_FOUND)
                );

        long impressions =
                adEventRepository
                        .countByCampaignIdAndEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                campaignId,
                                AdEventType.IMPRESSION,
                                start,
                                end
                        );

        long clicks =
                adEventRepository
                        .countByCampaignIdAndEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                campaignId,
                                AdEventType.CLICK,
                                start,
                                end
                        );

        double ctr = impressions == 0
                ? 0.0
                : Math.round(
                ((double) clicks / impressions * 100) * 100
        ) / 100.0;

        return new AdStatisticsResponse(
                campaign.getId(),
                campaign.getName(),
                impressions,
                clicks,
                ctr,
                campaign.getAdType()
        );
    }

    // 전체 캠페인 기간별 광고 통계 조회
    public List<AdStatisticsResponse> getAllCampaignStatisticsByPeriod(
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "조회 시작일은 종료일보다 이전이어야 합니다."
            );
        }

        return adCampaignRepository.findAll()
                .stream()
                .map(campaign ->
                        getCampaignStatisticsByPeriod(
                                campaign.getId(),
                                start,
                                end
                        )
                )
                .toList();
    }

    // 배너별 광고 통계 조회
    public AdBannerStatisticsResponse getBannerStatistics(Long bannerId) {

        AdBanner banner = adBannerRepository.findById(bannerId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.BANNER_NOT_FOUND)
                );

        long impressions = adEventRepository.countByBannerIdAndEventType(
                bannerId,
                AdEventType.IMPRESSION
        );

        long clicks = adEventRepository.countByBannerIdAndEventType(
                bannerId,
                AdEventType.CLICK
        );

        double ctr = impressions == 0
                ? 0.0
                : Math.round(
                ((double) clicks / impressions * 100) * 100
        ) / 100.0;

        return new AdBannerStatisticsResponse(
                banner.getId(),
                banner.getName(),
                impressions,
                clicks,
                ctr
        );
    }
    public record DailyStatistics(String date, long impressions, long clicks, double ctr) {}

    public List<DailyStatistics> getTrend(LocalDateTime start, LocalDateTime end,
            Long campaignId, com.needitbe.domain.campaign.entity.AdType adType) {
        if (!start.isBefore(end) || java.time.Duration.between(start, end).compareTo(java.time.Duration.ofDays(366)) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "조회 기간은 최대 366일입니다.");
        }
        var days = new java.util.LinkedHashMap<String, long[]>();
        for (var day = start.toLocalDate(); day.atStartOfDay().isBefore(end); day = day.plusDays(1)) {
            days.put(day.toString(), new long[2]);
        }
        for (Object[] row : adEventRepository.dailyStatistics(start, end, campaignId,
                adType == null ? null : adType.name())) {
            long[] counts = days.get(row[0].toString());
            if (counts == null) continue;
            if ("IMPRESSION".equals(row[1].toString())) counts[0] = ((Number) row[2]).longValue();
            if ("CLICK".equals(row[1].toString())) counts[1] = ((Number) row[2]).longValue();
        }
        return days.entrySet().stream().map(e -> new DailyStatistics(e.getKey(), e.getValue()[0],
                e.getValue()[1], e.getValue()[0] == 0 ? 0 : Math.round(e.getValue()[1] * 10000.0 / e.getValue()[0]) / 100.0)).toList();
    }
}