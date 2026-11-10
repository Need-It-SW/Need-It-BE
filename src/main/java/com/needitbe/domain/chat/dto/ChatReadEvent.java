package com.needitbe.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatReadEvent(
        Long chatRoomId,
        Long readerId,
        int readCount,
        LocalDateTime readAt
) {
}