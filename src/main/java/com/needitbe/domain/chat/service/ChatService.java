package com.needitbe.domain.chat.service;

import com.needitbe.domain.chat.dto.ChatMessageRequest;
import com.needitbe.domain.chat.dto.ChatMessageResponse;
import com.needitbe.domain.chat.dto.ChatRoomResponse;

import com.needitbe.domain.chat.entity.ChatMessage;
import com.needitbe.domain.chat.entity.ChatRoom;
import com.needitbe.domain.chat.entity.MessageType;

import com.needitbe.domain.chat.event.ChatImageSentEvent;
import com.needitbe.domain.chat.event.ChatReadCompletedEvent;

import com.needitbe.domain.chat.repository.ChatMessageRepository;
import com.needitbe.domain.chat.repository.ChatRoomRepository;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;

import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferStatus;

import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.needitbe.domain.notification.service.NotificationService;
import com.needitbe.domain.notification.entity.NotificationType;

import com.needitbe.domain.block.repository.BlockRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final BlockRepository blockRepository;
    private final NotificationService notificationService;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 내가 참여한 채팅방 목록
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long memberId) {

        List<ChatRoom> chatRooms =
                chatRoomRepository.findMyChatRooms(memberId);

        return chatRooms.stream()
                .map(room -> {

                    long unreadCount =
                            chatMessageRepository
                                    .countByChatRoomIdAndSenderIdNotAndReadAtIsNull(
                                            room.getId(),
                                            memberId
                                    );

                    return ChatRoomResponse.from(
                            room,
                            memberId,
                            unreadCount
                    );
                })
                .toList();
    }

    // 채팅 메시지 조회
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatMessages(
            Long memberId,
            Long chatRoomId
    ) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        Offer offer = chatRoom.getOffer();

        Long requestMemberId =
                offer.getItemRequest().getMember().getId();

        Long offerMemberId =
                offer.getMember().getId();

        if (!memberId.equals(requestMemberId)
                && !memberId.equals(offerMemberId)) {

            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        List<ChatMessage> messages =
                chatMessageRepository
                        .findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);

        return messages.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    // 채팅방 참여자 검증
    @Transactional(readOnly = true)
    public void validateChatParticipant(
            Long memberId,
            Long chatRoomId
    ) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        Offer offer = chatRoom.getOffer();

        Long requestMemberId =
                offer.getItemRequest().getMember().getId();

        Long offerMemberId =
                offer.getMember().getId();

        if (!memberId.equals(requestMemberId)
                && !memberId.equals(offerMemberId)) {

            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    // 텍스트 메시지 전송
    @Transactional
    public ChatMessageResponse sendMessage(
            Long memberId,
            Long chatRoomId,
            ChatMessageRequest request
    ) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        validateChatParticipant(memberId, chatRoomId);

        validateNotBlocked(chatRoom, memberId);

        Offer offer = chatRoom.getOffer();

        // 거래 완료 이후에도 채팅 가능
        if (offer.getStatus() != OfferStatus.ACCEPTED
                && offer.getStatus() != OfferStatus.COMPLETED) {

            throw new CustomException(ErrorCode.CHAT_NOT_ACTIVE);
        }

        if (request == null
                || request.content() == null
                || request.content().isBlank()
                || request.content().length() > 2000) {

            throw new IllegalArgumentException(
                    "메시지는 1~2000자로 입력해야 합니다."
            );
        }

        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        ChatMessage message = new ChatMessage(
                chatRoom,
                sender,
                MessageType.TEXT,
                request.content()
        );

        ChatMessage savedMessage =
                chatMessageRepository.saveAndFlush(message);

// ==========================================
// 채팅 상대방에게 알림 생성
// ==========================================

// 1. 알림 수신자 조회
        Long receiverId = getReceiverId(
                chatRoom,
                memberId
        );

// 2. 알림 DB 저장
        notificationService.createNotification(
                receiverId,
                NotificationType.CHAT_MESSAGE,
                "새로운 채팅 메시지가 도착했어요.",
                chatRoomId
        );

// 3. 기존 메시지 응답 반환
        return ChatMessageResponse.from(savedMessage);
    }

    // 채팅 메시지 읽음 처리
    @Transactional
    public int markMessagesAsRead(
            Long memberId,
            Long chatRoomId
    ) {

        validateChatParticipant(memberId, chatRoomId);

        List<ChatMessage> unreadMessages =
                chatMessageRepository
                        .findByChatRoomIdAndSenderIdNotAndReadAtIsNull(
                                chatRoomId,
                                memberId
                        );

        for (ChatMessage message : unreadMessages) {
            message.markAsRead();
        }

        int readCount = unreadMessages.size();

        if (readCount > 0) {

            eventPublisher.publishEvent(
                    new ChatReadCompletedEvent(
                            chatRoomId,
                            memberId,
                            readCount
                    )
            );
        }

        return readCount;
    }

    // 이미지 메시지 전송
    @Transactional
    public ChatMessageResponse sendImage(
            Long memberId,
            Long chatRoomId,
            MultipartFile file
    ) {

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        // 2. 채팅방 참여자 확인
        validateChatParticipant(memberId, chatRoomId);

        validateNotBlocked(chatRoom, memberId);

        // 3. 거래 상태 확인
        OfferStatus status = chatRoom.getOffer().getStatus();

        if (status != OfferStatus.ACCEPTED
                && status != OfferStatus.COMPLETED) {

            throw new CustomException(ErrorCode.CHAT_NOT_ACTIVE);
        }

        // 4. 발신자 조회
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // 5. S3 이미지 업로드
        String imageKey = s3Service.uploadChatImage(
                file,
                chatRoomId
        );

        try {

            // 6. 이미지 메시지 생성
            ChatMessage message = new ChatMessage(
                    chatRoom,
                    sender,
                    MessageType.IMAGE,
                    imageKey
            );

            // 7. DB 저장
            ChatMessage savedMessage =
                    chatMessageRepository.saveAndFlush(message);

            // 8. Presigned URL 생성
            ChatMessageResponse response =
                    toMessageResponse(savedMessage);

            // 9. DB 커밋 이후 WebSocket 전송
            // ==========================================
// 9. 채팅 상대방에게 알림 생성
// ==========================================

            Long receiverId = getReceiverId(
                    chatRoom,
                    memberId
            );

            notificationService.createNotification(
                    receiverId,
                    NotificationType.CHAT_MESSAGE,
                    "새로운 채팅 이미지가 도착했어요.",
                    chatRoomId
            );

// ==========================================
// 10. 기존 WebSocket 이벤트 발행
// ==========================================

            eventPublisher.publishEvent(
                    new ChatImageSentEvent(
                            chatRoomId,
                            response
                    )
            );

            return response;

        } catch (RuntimeException e) {

            // 오류 발생 시 S3 이미지 삭제 시도
            try {
                s3Service.deleteImage(imageKey);
            } catch (Exception deleteException) {
                e.addSuppressed(deleteException);
            }

            throw e;
        }
    }

    // 메시지 응답 변환
    private ChatMessageResponse toMessageResponse(
            ChatMessage message
    ) {

        if (message.getMessageType() == MessageType.IMAGE) {

            String imageUrl = s3Service.getPresignedUrl(
                    message.getContent()
            );

            return ChatMessageResponse.from(
                    message,
                    imageUrl
            );
        }

        return ChatMessageResponse.from(message);
    }

    // 채팅 상대방 ID 조회
    private Long getReceiverId(
            ChatRoom chatRoom,
            Long senderId
    ) {

        Offer offer = chatRoom.getOffer();

        Long requestMemberId =
                offer.getItemRequest().getMember().getId();

        Long offerMemberId =
                offer.getMember().getId();

        if (senderId.equals(requestMemberId)) {
            return offerMemberId;
        }

        if (senderId.equals(offerMemberId)) {
            return requestMemberId;
        }

        throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
    }

    // 채팅 상대방과 차단 관계 확인
    private void validateNotBlocked(ChatRoom chatRoom, Long senderId) {
        Long receiverId = getReceiverId(chatRoom, senderId);

        if (blockRepository.existsBlockBetweenMembers(senderId, receiverId)) {
            throw new CustomException(ErrorCode.MEMBER_BLOCKED);
        }
    }
}