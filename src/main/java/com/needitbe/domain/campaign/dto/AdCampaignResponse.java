package com.needitbe.domain.campaign.dto;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.entity.CampaignStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdCampaignResponse(

        Long campaignId,
        String name,
        AdType adType,

        String targetCategory,
        String targetAction,
        String targetKeywords,
        String targetRegions,

        int maxImpressions,
        String impressionPeriod,

        LocalDate startDate,
        LocalDate endDate,

        String description,
        boolean active,

        CampaignStatus status,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static AdCampaignResponse from(AdCampaign campaign) {

        return new AdCampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getAdType(),

                campaign.getTargetCategory(),
                campaign.getTargetAction(),
                campaign.getTargetKeywords(),
                campaign.getTargetRegions(),

                campaign.getMaxImpressions(),
                campaign.getImpressionPeriod(),

                campaign.getStartDate(),
                campaign.getEndDate(),

                campaign.getDescription(),
                campaign.isActive(),

                campaign.getStatus(),

                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}