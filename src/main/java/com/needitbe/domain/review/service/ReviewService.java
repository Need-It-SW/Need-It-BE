package com.needitbe.domain.review.service;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferStatus;
import com.needitbe.domain.offer.repository.OfferRepository;

import com.needitbe.domain.review.dto.ReviewCreateRequest;
import com.needitbe.domain.review.dto.ReviewResponse;
import com.needitbe.domain.review.dto.ReviewUpdateRequest;

import com.needitbe.domain.review.entity.Review;
import com.needitbe.domain.review.repository.ReviewRepository;

import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.needitbe.domain.review.dto.ReviewSummaryResponse;
import com.needitbe.domain.review.entity.ReviewRating;
import com.needitbe.domain.member.repository.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final MemberRepository memberRepository;

    private final ReviewRepository reviewRepository;
    private final OfferRepository offerRepository;

    // ==========================================
    // 1. 후기 작성
    // ==========================================
    @Transactional
    public ReviewResponse createReview(
            Long memberId,
            ReviewCreateRequest request
    ) {

        // 1. 거래 조회
        Offer offer = offerRepository.findById(request.offerId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.OFFER_NOT_FOUND)
                );

        // 2. 거래 완료 여부 확인
        if (offer.getStatus() != OfferStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "완료된 거래에만 후기를 작성할 수 있습니다."
            );
        }

        // 3. 요청자와 도움 제공자 조회
        Member requester = offer.getItemRequest().getMember();
        Member provider = offer.getMember();

        // 4. 후기 작성자 확인 및 상대방 결정
        Member reviewer;
        Member reviewee;

        if (requester.getId().equals(memberId)) {

            reviewer = requester;
            reviewee = provider;

        } else if (provider.getId().equals(memberId)) {

            reviewer = provider;
            reviewee = requester;

        } else {

            throw new IllegalArgumentException(
                    "거래에 참여한 회원만 후기를 작성할 수 있습니다."
            );
        }

        // 5. 중복 후기 확인
        if (reviewRepository.existsByOfferIdAndReviewerId(
                offer.getId(),
                memberId
        )) {
            throw new IllegalArgumentException(
                    "이미 후기를 작성한 거래입니다."
            );
        }

        // 6. 후기 저장
        Review review = new Review(
                offer,
                reviewer,
                reviewee,
                request.rating(),
                request.content()
        );

        reviewRepository.save(review);

        return ReviewResponse.from(review);
    }

    // ==========================================
    // 2. 회원이 받은 후기 조회
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReceivedReviews(Long memberId) {

        return reviewRepository
                .findByRevieweeIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // ==========================================
    // 3. 내가 작성한 후기 조회
    // ==========================================
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long memberId) {

        return reviewRepository
                .findByReviewerIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // ==========================================
    // 4. 후기 수정
    // ==========================================
    @Transactional
    public ReviewResponse updateReview(
            Long memberId,
            Long reviewId,
            ReviewUpdateRequest request
    ) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "후기를 찾을 수 없습니다."
                        )
                );

        if (!review.getReviewer().getId().equals(memberId)) {
            throw new IllegalArgumentException(
                    "본인이 작성한 후기만 수정할 수 있습니다."
            );
        }

        review.update(
                request.rating(),
                request.content()
        );

        // 변경 사항을 DB에 즉시 반영
        reviewRepository.flush();

        return ReviewResponse.from(review);
    }

    // ==========================================
    // 5. 후기 삭제
    // ==========================================
    @Transactional
    public void deleteReview(
            Long memberId,
            Long reviewId
    ) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "후기를 찾을 수 없습니다."
                        )
                );

        if (!review.getReviewer().getId().equals(memberId)) {
            throw new IllegalArgumentException(
                    "본인이 작성한 후기만 삭제할 수 있습니다."
            );
        }

        reviewRepository.delete(review);
    }

    // ==========================================
// 6. 회원별 후기 통계 조회
// ==========================================
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Long memberId) {

        // 1. 회원 존재 여부 확인
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 2. 전체 받은 후기 개수
        long totalReviews =
                reviewRepository.countByRevieweeId(memberId);

        // 3. 좋아요 개수
        long goodCount =
                reviewRepository.countByRevieweeIdAndRating(
                        memberId,
                        ReviewRating.GOOD
                );

        // 4. 보통 개수
        long normalCount =
                reviewRepository.countByRevieweeIdAndRating(
                        memberId,
                        ReviewRating.NORMAL
                );

        // 5. 아쉬워요 개수
        long badCount =
                reviewRepository.countByRevieweeIdAndRating(
                        memberId,
                        ReviewRating.BAD
                );

        // 6. 긍정 후기 비율 계산
        double goodRate = totalReviews == 0
                ? 0.0
                : Math.round(
                (double) goodCount / totalReviews * 1000
        ) / 10.0;

        // 7. 응답 반환
        return new ReviewSummaryResponse(
                memberId,
                totalReviews,
                goodCount,
                normalCount,
                badCount,
                goodRate
        );
    }
}