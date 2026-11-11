package com.needitbe.domain.request.dto;

import com.needitbe.domain.request.entity.ItemRequestImage;

public record RequestImageResponse(

        Long imageId,
        String imageUrl,
        Integer sortOrder

) {

    public static RequestImageResponse from(
            ItemRequestImage image,
            String presignedUrl
    ) {

        return new RequestImageResponse(
                image.getId(),
                presignedUrl,
                image.getSortOrder()
        );
    }
}