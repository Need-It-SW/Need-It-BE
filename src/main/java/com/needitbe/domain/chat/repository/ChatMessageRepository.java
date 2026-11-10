package com.needitbe.domain.chat.repository;

import com.needitbe.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방 메시지 조회
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(
            Long chatRoomId
    );

    // 채팅방 전체 메시지 개수
    long countByChatRoomId(Long chatRoomId);

    // 상대방이 보낸 읽지 않은 메시지 조회
    List<ChatMessage> findByChatRoomIdAndSenderIdNotAndReadAtIsNull(
            Long chatRoomId,
            Long memberId
    );

    // 상대방이 보낸 읽지 않은 메시지 개수
    long countByChatRoomIdAndSenderIdNotAndReadAtIsNull(
            Long chatRoomId,
            Long memberId
    );
}