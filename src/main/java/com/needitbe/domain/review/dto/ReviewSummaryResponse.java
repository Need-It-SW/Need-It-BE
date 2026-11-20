package com.needitbe.domain.review.dto;

public record ReviewSummaryResponse(
        Long memberId,
        long totalReviews,
        long goodCount,
        long normalCount,
        long badCount,
        double goodRate
) {
}