package com.needitbe.domain.banner.dto;

import com.needitbe.domain.banner.entity.AdBanner;

import java.time.LocalDateTime;

public record AdBannerResponse(
        Long bannerId,
        String name,
        String imageUrl,
        String targetUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdBannerResponse from(
            AdBanner banner,
            String imageUrl
    ) {
        return new AdBannerResponse(
                banner.getId(),
                banner.getName(),
                imageUrl,
                banner.getTargetUrl(),
                banner.isActive(),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }
}