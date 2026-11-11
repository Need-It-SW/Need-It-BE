package com.needitbe.domain.favorite.dto;

import com.needitbe.domain.favorite.entity.Favorite;
import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.request.entity.RequestStatus;

import java.time.LocalDateTime;

public record FavoriteResponse(

        Long favoriteId,
        Long itemRequestId,
        String title,
        RequestCategory category,
        String region,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime favoritedAt

) {

    public static FavoriteResponse from(Favorite favorite) {

        ItemRequest itemRequest = favorite.getItemRequest();

        return new FavoriteResponse(
                favorite.getId(),
                itemRequest.getId(),
                itemRequest.getTitle(),
                itemRequest.getCategory(),
                itemRequest.getRegion(),
                itemRequest.getStatus(),
                itemRequest.getCreatedAt(),
                favorite.getCreatedAt()
        );
    }
}