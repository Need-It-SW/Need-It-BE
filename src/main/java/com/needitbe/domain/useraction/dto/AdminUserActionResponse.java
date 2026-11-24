package com.needitbe.domain.useraction.dto.response;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.entity.UserActionFilterResult;
import com.needitbe.domain.useraction.entity.UserActionFilterReason;

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

        UserActionFilterResult filterResult,
        UserActionFilterReason filterReason,
        Long sanctionId,
        String sanctionReason,
        LocalDateTime filteredAt,

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
            List<MatchedCampaignResponse> matchedCampaigns,
            String sanctionReason
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

                log.getFilterResult(),
                log.getFilterReason(),
                log.getSanctionId(),
                sanctionReason,
                log.getFilteredAt(),

                log.getCreatedAt()
        );
    }
}