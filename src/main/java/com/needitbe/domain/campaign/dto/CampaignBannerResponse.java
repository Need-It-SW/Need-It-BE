package com.needitbe.domain.campaign.dto;

import com.needitbe.domain.campaign.entity.CampaignBanner;

public record CampaignBannerResponse(

        Long id,
        Long campaignId,
        Long bannerId,
        String bannerName,
        String imageUrl,
        String placement,
        boolean active

) {

    public static CampaignBannerResponse from(
            CampaignBanner campaignBanner,
            String imageUrl
    ) {
        return new CampaignBannerResponse(
                campaignBanner.getId(),
                campaignBanner.getCampaign().getId(),
                campaignBanner.getBanner().getId(),
                campaignBanner.getBanner().getName(),
                imageUrl,
                campaignBanner.getPlacement(),
                campaignBanner.getBanner().isActive()
        );
    }
}