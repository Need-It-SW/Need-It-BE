package com.needitbe.domain.admin.dto;

import com.needitbe.domain.ads.entity.AdEventType;

import java.time.LocalDateTime;

public record FilterAdEventResponse(
        Long eventId,
        Long campaignId,
        Long bannerId,
        Long memberId,
        AdEventType eventType,
        String impressionId,
        String placement,
        LocalDateTime createdAt
) {
}