package com.needitbe.domain.admin.dto;

import com.needitbe.domain.ads.entity.AdExclusionReasonType;

import java.time.LocalDateTime;
import java.util.List;

public record FilterExclusionResponse(

        Long evaluationId,

        Long campaignId,

        Long memberId,

        LocalDateTime createdAt,

        List<ExclusionReasonDetail> reasons

) {

    public record ExclusionReasonDetail(

            AdExclusionReasonType reason,

            String detail

    ) {
    }
}