package com.needitbe.domain.offer.dto;

import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferStatus;

import java.time.LocalDateTime;

public record OfferResponse(

        Long offerId,
        Long requestId,
        Long memberId,
        String nickname,
        String itemDescription,
        String message,
        OfferStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static OfferResponse from(Offer offer) {

        return new OfferResponse(
                offer.getId(),
                offer.getItemRequest().getId(),
                offer.getMember().getId(),
                offer.getMember().getNickname(),
                offer.getItemDescription(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}