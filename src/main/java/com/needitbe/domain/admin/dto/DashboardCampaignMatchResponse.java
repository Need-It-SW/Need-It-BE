package com.needitbe.domain.admin.dto;

import com.needitbe.domain.campaign.entity.CampaignStatus;

public record DashboardCampaignMatchResponse(
        Long campaignId,
        String campaignName,
        CampaignStatus status,
        String targetCategory,
        long matchCount
) {
}