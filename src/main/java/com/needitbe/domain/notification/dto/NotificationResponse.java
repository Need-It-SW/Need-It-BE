package com.needitbe.domain.notification.dto;

import com.needitbe.domain.notification.entity.Notification;
import com.needitbe.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long notificationId,
        NotificationType type,
        String content,
        Long targetId,
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt

) {

    public static NotificationResponse from(
            Notification notification
    ) {

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.getTargetId(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}