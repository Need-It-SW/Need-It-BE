package com.needitbe.domain.request.dto;

import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.request.entity.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RequestResponse(

        Long requestId,
        Long memberId,
        String nickname,
        String title,
        String description,
        RequestCategory category,
        String region,
        RequestStatus status,
        List<RequestImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

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