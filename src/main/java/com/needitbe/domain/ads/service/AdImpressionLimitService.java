package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.entity.AdEventType;
import com.needitbe.domain.ads.repository.AdEventRepository;
import com.needitbe.domain.campaign.entity.AdCampaign;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdImpressionLimitService {

    private final AdEventRepository adEventRepository;

    @Transactional(readOnly = true)
    public boolean canImpress(
            AdCampaign campaign,
            Long memberId
    ) {

        int maxImpressions = campaign.getMaxImpressions();

        // 비로그인 사용자는 회원별 노출 제한을 적용하지 않음
        if (memberId == null) {
            return true;
        }

        long currentImpressions =
                countImpressions(campaign, memberId);

        return currentImpressions < maxImpressions;
    }

    @Transactional(readOnly = true)
    public long countImpressions(
            AdCampaign campaign,
            Long memberId
    ) {

        if (memberId == null) {
            return 0;
        }

        String period = campaign.getImpressionPeriod();

        if (period == null) {
            throw new IllegalArgumentException(
                    "노출 집계 기간이 설정되지 않았습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        return switch (period) {

            // 캠페인 전체 기간
            case "TOTAL", "ALL" ->
                    countAll(campaign.getId(), memberId);

            // 최근 24시간
            case "DAILY", "LAST_24_HOURS" ->
                    countSince(
                            campaign.getId(),
                            memberId,
                            now.minusHours(24)
                    );

            // 최근 7일
            case "WEEKLY", "LAST_7_DAYS" ->
                    countSince(
                            campaign.getId(),
                            memberId,
                            now.minusDays(7)
                    );

            default -> throw new IllegalArgumentException(
                    "지원하지 않는 노출 집계 기간: " + period
            );
        };
    }

    private long countAll(
            Long campaignId,
            Long memberId
    ) {

        return adEventRepository
                .countByCampaignIdAndMemberIdAndEventType(
                        campaignId,
                        memberId,
                        AdEventType.IMPRESSION
                );
    }

    private long countSince(
            Long campaignId,
            Long memberId,
            LocalDateTime start
    ) {

        return adEventRepository
                .countByCampaignIdAndMemberIdAndEventTypeAndCreatedAtGreaterThanEqual(
                        campaignId,
                        memberId,
                        AdEventType.IMPRESSION,
                        start
                );
    }
}