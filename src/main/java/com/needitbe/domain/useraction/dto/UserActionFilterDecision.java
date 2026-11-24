package com.needitbe.domain.useraction.dto;

import com.needitbe.domain.useraction.entity.UserActionFilterReason;
import com.needitbe.domain.useraction.entity.UserActionFilterResult;

public record UserActionFilterDecision(
        UserActionFilterResult result,
        UserActionFilterReason reason,
        Long sanctionId
) {

    public static UserActionFilterDecision included() {
        return new UserActionFilterDecision(
                UserActionFilterResult.INCLUDED,
                UserActionFilterReason.NONE,
                null
        );
    }

    public static UserActionFilterDecision excluded(
            UserActionFilterReason reason,
            Long sanctionId
    ) {
        return new UserActionFilterDecision(
                UserActionFilterResult.EXCLUDED,
                reason,
                sanctionId
        );
    }
}