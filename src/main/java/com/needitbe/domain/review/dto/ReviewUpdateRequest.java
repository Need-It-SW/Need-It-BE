package com.needitbe.domain.review.dto;

import com.needitbe.domain.review.entity.ReviewRating;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(

        @NotNull
        ReviewRating rating,

        @Size(max = 200)
        String content

) {
}