package com.needitbe.domain.chat.controller;

import com.needitbe.domain.chat.dto.ChatMessageResponse;
import com.needitbe.domain.chat.dto.ChatReadResponse;
import com.needitbe.domain.chat.dto.ChatRoomResponse;
import com.needitbe.domain.chat.service.ChatService;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    // 내가 참여한 채팅방 목록
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyChatRooms(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<ChatRoomResponse> response =
                chatService.getMyChatRooms(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "채팅방 목록 조회 성공",
                        response
                )
        );
    }

    // 채팅 메시지 조회
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatMessages(
            @PathVariable Long chatRoomId,
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<ChatMessageResponse> response =
                chatService.getChatMessages(memberId, chatRoomId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "채팅 메시지 조회 성공",
                        response
                )
        );
    }

    // 채팅 메시지 읽음 처리
    @PatchMapping("/{chatRoomId}/read")
    public ResponseEntity<ApiResponse<ChatReadResponse>> markMessagesAsRead(
            @PathVariable Long chatRoomId,
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        int readCount = chatService.markMessagesAsRead(
                memberId,
                chatRoomId
        );

        ChatReadResponse response = new ChatReadResponse(
                chatRoomId,
                readCount
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "채팅 메시지 읽음 처리 성공",
                        response
                )
        );
    }

    // 채팅 이미지 전송
    @PostMapping(
            value = "/{chatRoomId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendImage(
            @PathVariable Long chatRoomId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        ChatMessageResponse response =
                chatService.sendImage(
                        memberId,
                        chatRoomId,
                        file
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "채팅 이미지 전송 성공",
                        response
                )
        );
    }
}