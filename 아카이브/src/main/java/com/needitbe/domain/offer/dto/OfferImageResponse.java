package com.needitbe.domain.offer.dto;

import com.needitbe.domain.offer.entity.OfferImage;

public record OfferImageResponse(

        Long imageId,
        String imageUrl,
        Integer sortOrder

) {

    public static OfferImageResponse from(
            OfferImage image,
            String presignedUrl
    ) {
        return new OfferImageResponse(
                image.getId(),
                presignedUrl,
                image.getSortOrder()
        );
    }
}