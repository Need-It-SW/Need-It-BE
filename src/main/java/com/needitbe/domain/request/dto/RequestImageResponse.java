package com.needitbe.domain.request.dto;

import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.entity.ItemRequestImage;

import java.util.List;

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
    public static RequestResponse from(
            ItemRequest request,
            List<RequestImageResponse> images
    ) {
        return new RequestResponse(
                request.getId(),
                request.getMember().getId(),
                request.getMember().getNickname(),
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getRegion(),
                request.getStatus(),
                images,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}