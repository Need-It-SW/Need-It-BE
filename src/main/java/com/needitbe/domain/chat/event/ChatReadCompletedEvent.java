package com.needitbe.domain.chat.event;

public record ChatReadCompletedEvent(
        Long chatRoomId,
        Long readerId,
        int readCount
) {
}