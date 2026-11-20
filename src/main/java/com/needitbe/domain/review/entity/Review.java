package com.needitbe.domain.review.entity;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.offer.entity.Offer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_offer_reviewer",
                        columnNames = {"offer_id", "reviewer_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_review_reviewee",
                        columnList = "reviewee_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 거래 제안
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    // 후기 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Member reviewer;

    // 후기 받는 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private Member reviewee;

    // 평가
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewRating rating;

    // 후기 내용 (선택)
    @Column(length = 200)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Review(
            Offer offer,
            Member reviewer,
            Member reviewee,
            ReviewRating rating,
            String content
    ) {
        this.offer = offer;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.rating = rating;
        this.content = content;
    }

    public void update(
            ReviewRating rating,
            String content
    ) {
        this.rating = rating;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}