package com.needitbe.domain.ads.dto;

import com.needitbe.domain.campaign.entity.CampaignBanner;

public record AdResponse(
        Long campaignId,
        Long bannerId,
        String bannerName,
        String imageUrl,
        String targetUrl,
        String placement,
        String impressionId
) {

    public static AdResponse from(
            CampaignBanner campaignBanner,
            String imageUrl,
            String impressionId
    ) {

        return new AdResponse(
                campaignBanner.getCampaign().getId(),
                campaignBanner.getBanner().getId(),
                campaignBanner.getBanner().getName(),
                imageUrl,
                campaignBanner.getBanner().getTargetUrl(),
                campaignBanner.getPlacement(),
                impressionId
        );
    }
}