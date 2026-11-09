package com.needitbe.domain.offer.entity;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.request.entity.ItemRequest;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제안 대상 요청 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_request_id", nullable = false)
    private ItemRequest itemRequest;

    // 제안 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 나눔할 물품 설명
    @Column(name = "item_description", nullable = false, length = 200)
    private String itemDescription;

    // 전달할 메시지
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // 제안 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;

    // 생성일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Offer(
            ItemRequest itemRequest,
            Member member,
                String itemDescription,
                String message
    ) {
        this.itemRequest = itemRequest;
        this.member = member;
        this.itemDescription = itemDescription;
        this.message = message;
        this.status = OfferStatus.PENDING;
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

    // 나눔 제안 이미지 목록
    @OneToMany(
            mappedBy = "offer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    private List<OfferImage> images = new ArrayList<>();

    public void addImage(
            String imageUrl,
            String imageKey,
            Integer sortOrder
    ) {
        OfferImage image = new OfferImage(
                this,
                imageUrl,
                imageKey,
                sortOrder
        );

        this.images.add(image);
    }

    public void removeImage(OfferImage image) {
        this.images.remove(image);
    }

    // 제안 수락
    public void accept() {
        this.status = OfferStatus.ACCEPTED;
    }

    // 제안 거절
    public void reject() {
        this.status = OfferStatus.REJECTED;
    }

    // 제안 취소
    public void cancel() {
        this.status = OfferStatus.CANCELED;
    }

    // 수락 취소: 다시 수락할 수 없도록 거절 상태로 변경
    public void cancelAcceptance() {
        this.status = OfferStatus.REJECTED;
    }

    // 거래 완료
    public void complete() {
        this.status = OfferStatus.COMPLETED;
    }
}