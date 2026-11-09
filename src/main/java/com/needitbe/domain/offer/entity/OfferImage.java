package com.needitbe.domain.offer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "offer_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이미지가 속한 나눔 제안
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    // S3 이미지 주소
    @Column(nullable = false, length = 1000)
    private String imageUrl;

    // S3 객체 키
    @Column(nullable = false, length = 500)
    private String imageKey;

    // 이미지 표시 순서
    @Column(nullable = false)
    private Integer sortOrder;

    // 생성일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OfferImage(
            Offer offer,
            String imageUrl,
            String imageKey,
            Integer sortOrder
    ) {
        this.offer = offer;
        this.imageUrl = imageUrl;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}