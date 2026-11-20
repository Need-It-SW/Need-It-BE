package com.needitbe.domain.admin.dto;

import com.needitbe.domain.ads.entity.AdExclusionReasonType;

public record FilterReasonResponse(

        AdExclusionReasonType reason,

        long count

) {
}