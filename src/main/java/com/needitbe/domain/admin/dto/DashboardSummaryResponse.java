package com.needitbe.domain.admin.dto;

public record DashboardSummaryResponse(
        long totalActionLogs,
        long totalCampaigns,
        long runningCampaigns,
        long matchedActionLogs
) {
}