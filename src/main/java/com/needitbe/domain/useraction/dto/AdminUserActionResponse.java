package com.needitbe.domain.useraction.dto.response;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.entity.UserActionLog;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserActionResponse(

        Long id,
        Long memberId,
        String action,
        String keyword,
        Long requestId,
        RequestCategory category,
        String region,
        List<MatchedCampaignResponse> matchedCampaigns,
        LocalDateTime createdAt

) {

    public record MatchedCampaignResponse(
            Long campaignId,
            String campaignName
    ) {
    }

    public static AdminUserActionResponse from(
            UserActionLog log,
            String region,
            List<MatchedCampaignResponse> matchedCampaigns
    ) {

        return new AdminUserActionResponse(
                log.getId(),
                log.getMemberId(),
                log.getAction(),
                log.getKeyword(),
                log.getRequestId(),
                log.getCategory(),
                region,
                matchedCampaigns,
                log.getCreatedAt()
        );
    }
}