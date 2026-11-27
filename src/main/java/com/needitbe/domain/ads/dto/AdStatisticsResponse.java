package com.needitbe.domain.ads.dto;

public record AdStatisticsResponse(
        Long campaignId,
        String campaignName,
        long impressions,
        long clicks,
        double ctr,
        com.needitbe.domain.campaign.entity.AdType adType
) {
}