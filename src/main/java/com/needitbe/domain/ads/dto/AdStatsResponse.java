package com.needitbe.domain.ads.dto;

public record AdStatsResponse(
        Long campaignId,
        long impressions,
        long clicks,
        double ctr
) {
}