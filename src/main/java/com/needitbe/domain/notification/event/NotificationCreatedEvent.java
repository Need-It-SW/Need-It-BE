package com.needitbe.domain.notification.event;

import com.needitbe.domain.notification.dto.NotificationResponse;

public record NotificationCreatedEvent(
        Long receiverId,
        NotificationResponse notification
) {
}