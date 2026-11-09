package com.needitbe.domain.offer.entity;

import com.needitbe.domain.member.entity.Member;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "offer_acceptance_cancellations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfferAcceptanceCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수락 취소된 제안
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    // 수락 취소를 실행한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 취소 사유
    @Column(nullable = false, length = 100)
    private String reason;

    // 취소 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OfferAcceptanceCancellation(
            Offer offer,
            Member member,
            String reason
    ) {
        this.offer = offer;
        this.member = member;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}