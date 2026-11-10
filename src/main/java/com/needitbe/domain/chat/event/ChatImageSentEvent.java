package com.needitbe.domain.chat.event;

import com.needitbe.domain.chat.dto.ChatMessageResponse;

public record ChatImageSentEvent(
        Long chatRoomId,
        ChatMessageResponse message
) {
}