package com.needitbe.domain.offer.dto;

import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OfferSentResponse(

        Long offerId,
        Long requestId,
        String requestTitle,
        String itemDescription,
        String message,
        OfferStatus status,
        List<OfferImageResponse> images,
        LocalDateTime createdAt

) {

    public static OfferSentResponse from(
            Offer offer,
            List<OfferImageResponse> images
    ) {

        return new OfferSentResponse(
                offer.getId(),
                offer.getItemRequest().getId(),
                offer.getItemRequest().getTitle(),
                offer.getItemDescription(),
                offer.getMessage(),
                offer.getStatus(),
                images,
                offer.getCreatedAt()
        );
    }
}