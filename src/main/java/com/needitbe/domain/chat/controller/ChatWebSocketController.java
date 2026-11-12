package com.needitbe.domain.chat.controller;

import com.needitbe.domain.chat.dto.ChatMessageRequest;
import com.needitbe.domain.chat.dto.ChatMessageResponse;
import com.needitbe.domain.chat.service.ChatService;

import com.needitbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;

import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // 채팅 메시지 전송
    @MessageMapping("/chats/{chatRoomId}/send")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Payload ChatMessageRequest request,
            Principal principal
    ) {

        if (principal == null) {
            throw new IllegalArgumentException(
                    "인증되지 않은 사용자입니다."
            );
        }

        Long memberId = Long.valueOf(principal.getName());

        // 메시지 저장
        ChatMessageResponse response =
                chatService.sendMessage(
                        memberId,
                        chatRoomId,
                        request
                );

        // 채팅방 구독자에게 실시간 전송
        messagingTemplate.convertAndSend(
                "/topic/chats/" + chatRoomId,
                response
        );
    }

    // WebSocket 메시지 처리 중 발생한 오류를 발신자에게 전달
    @MessageExceptionHandler({
            IllegalArgumentException.class,
            CustomException.class
    })
    @SendToUser(value = "/queue/errors", broadcast = false)
    public Map<String, String> handleChatException(Exception e) {

        String message = e instanceof CustomException customException
                ? customException.getErrorCode().getMessage()
                : e.getMessage();

        return Map.of(
                "type", "CHAT_ERROR",
                "message", message
        );
    }
}