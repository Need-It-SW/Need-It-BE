package com.needitbe.domain.review.controller;

import com.needitbe.domain.review.dto.ReviewCreateRequest;
import com.needitbe.domain.review.dto.ReviewResponse;
import com.needitbe.domain.review.dto.ReviewUpdateRequest;
import com.needitbe.domain.review.service.ReviewService;

import com.needitbe.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.needitbe.domain.review.dto.ReviewSummaryResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    // ==========================================
    // 1. 후기 작성
    // ==========================================
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            Authentication authentication,
            @Valid @RequestBody ReviewCreateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        ReviewResponse response =
                reviewService.createReview(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "후기가 등록되었습니다.",
                                response
                        )
                );
    }

    // ==========================================
    // 2. 회원이 받은 후기 조회
    // ==========================================
    @GetMapping("/members/{memberId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReceivedReviews(
            @PathVariable Long memberId
    ) {

        List<ReviewResponse> response =
                reviewService.getReceivedReviews(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원이 받은 후기 조회에 성공했습니다.",
                        response
                )
        );
    }

    // ==========================================
    // 3. 내가 작성한 후기 조회
    // ==========================================
    @GetMapping("/reviews/me")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<ReviewResponse> response =
                reviewService.getMyReviews(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내가 작성한 후기 조회에 성공했습니다.",
                        response
                )
        );
    }

    // ==========================================
    // 4. 후기 수정
    // ==========================================
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            Authentication authentication,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        ReviewResponse response =
                reviewService.updateReview(
                        memberId,
                        reviewId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "후기가 수정되었습니다.",
                        response
                )
        );
    }

    // ==========================================
    // 5. 후기 삭제
    // ==========================================
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            Authentication authentication,
            @PathVariable Long reviewId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        reviewService.deleteReview(memberId, reviewId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "후기가 삭제되었습니다.",
                        null
                )
        );
    }

    // ==========================================
// 6. 회원별 후기 통계 조회
// ==========================================
    @GetMapping("/members/{memberId}/review-summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummary(
            @PathVariable Long memberId
    ) {

        ReviewSummaryResponse response =
                reviewService.getReviewSummary(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 후기 통계 조회에 성공했습니다.",
                        response
                )
        );
    }
}