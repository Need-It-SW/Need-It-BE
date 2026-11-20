package com.needitbe.domain.review.repository;

import com.needitbe.domain.review.entity.Review;
import com.needitbe.domain.review.entity.ReviewRating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 동일 거래 중복 후기 확인
    boolean existsByOfferIdAndReviewerId(
            Long offerId,
            Long reviewerId
    );

    // 회원이 받은 후기
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(
            Long revieweeId
    );

    // 내가 작성한 후기
    List<Review> findByReviewerIdOrderByCreatedAtDesc(
            Long reviewerId
    );

    // 평가별 통계
    long countByRevieweeIdAndRating(
            Long revieweeId,
            ReviewRating rating
    );

    // 전체 받은 후기 수
    long countByRevieweeId(Long revieweeId);
}