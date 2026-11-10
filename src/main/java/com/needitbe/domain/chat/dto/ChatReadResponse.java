package com.needitbe.domain.chat.dto;

public record ChatReadResponse(
        Long chatRoomId,
        int readCount
) {
}