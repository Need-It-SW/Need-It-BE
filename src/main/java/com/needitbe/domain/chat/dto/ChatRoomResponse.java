package com.needitbe.domain.chat.dto;

import com.needitbe.domain.chat.entity.ChatRoom;
import com.needitbe.domain.offer.entity.OfferStatus;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long offerId,
        Long requestId,
        String requestTitle,
        Long opponentId,
        String opponentNickname,
        OfferStatus offerStatus,
        boolean canSendMessage,
        long unreadCount,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(
            ChatRoom chatRoom,
            Long memberId,
            long unreadCount
    ) {

        var offer = chatRoom.getOffer();
        var itemRequest = offer.getItemRequest();

        var requestMember = itemRequest.getMember();
        var offerMember = offer.getMember();

        // 상대방 결정
        var opponent = requestMember.getId().equals(memberId)
                ? offerMember
                : requestMember;

        return new ChatRoomResponse(
                chatRoom.getId(),
                offer.getId(),
                itemRequest.getId(),
                itemRequest.getTitle(),
                opponent.getId(),
                opponent.getNickname(),
                offer.getStatus(),
                offer.getStatus() == OfferStatus.ACCEPTED
                        || offer.getStatus() == OfferStatus.COMPLETED,
                unreadCount,
                chatRoom.getCreatedAt()
        );
    }
}