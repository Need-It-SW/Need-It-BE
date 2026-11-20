package com.needitbe.domain.review.dto;

import com.needitbe.domain.review.entity.Review;
import com.needitbe.domain.review.entity.ReviewRating;

import java.time.LocalDateTime;

public record ReviewResponse(

        Long reviewId,
        Long offerId,

        Long reviewerId,
        Long revieweeId,

        ReviewRating rating,
        String content,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static ReviewResponse from(Review review) {

        return new ReviewResponse(
                review.getId(),
                review.getOffer().getId(),

                review.getReviewer().getId(),
                review.getReviewee().getId(),

                review.getRating(),
                review.getContent(),

                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}