package com.needitbe.domain.ads.dto;

public record AdBannerStatisticsResponse(
        Long bannerId,
        String bannerName,
        long impressions,
        long clicks,
        double ctr
) {
}