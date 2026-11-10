package com.needitbe.domain.chat.dto;

import com.needitbe.domain.chat.entity.ChatMessage;
import com.needitbe.domain.chat.entity.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String senderNickname,
        MessageType messageType,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    // 기존 텍스트 메시지 응답
    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, message.getContent());
    }

    // 이미지 URL을 전달받는 응답
    public static ChatMessageResponse from(
            ChatMessage message,
            String content
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getNickname(),
                message.getMessageType(),
                content,
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}