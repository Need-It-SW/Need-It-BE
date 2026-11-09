package com.needitbe.domain.offer.dto;

import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OfferReceivedResponse(
        Long offerId,
        Long requestId,
        String requestTitle,
        Long memberId,
        String nickname,
        String itemDescription,
        String message,
        OfferStatus status,
        List<OfferImageResponse> images,
        LocalDateTime createdAt
) {
    public static OfferReceivedResponse from(
            Offer offer,
            List<OfferImageResponse> images
    ) {
        return new OfferReceivedResponse(
                offer.getId(),
                offer.getItemRequest().getId(),
                offer.getItemRequest().getTitle(),
                offer.getMember().getId(),
                offer.getMember().getNickname(),
                offer.getItemDescription(),
                offer.getMessage(),
                offer.getStatus(),
                images,
                offer.getCreatedAt()
        );
    }
}