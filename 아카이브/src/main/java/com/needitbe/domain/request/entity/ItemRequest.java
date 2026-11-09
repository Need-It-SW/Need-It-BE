package com.needitbe.domain.request.entity;

import com.needitbe.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "item_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 게시글 제목
    @Column(nullable = false, length = 100)
    private String title;

    // 게시글 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestCategory category;

    // 요청 지역
    @Column(nullable = false, length = 100)
    private String region;

    // 요청 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    // 작성일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 게시글 생성
    public ItemRequest(
            Member member,
            String title,
            String description,
            RequestCategory category,
            String region
    ) {
        this.member = member;
        this.title = title;
        this.description = description;
        this.category = category;
        this.region = region;
        this.status = RequestStatus.OPEN;
    }

    // 최초 저장 시 실행
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    // 수정 시 실행
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 게시글 수정
    public void update(
            String title,
            String description,
            RequestCategory category,
            String region
    ) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.region = region;
    }

    // 요청 마감
    public void close() {
        this.status = RequestStatus.CLOSED;
    }

    // 게시글 이미지 목록
    @OneToMany(
            mappedBy = "itemRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    private List<ItemRequestImage> images = new ArrayList<>();

    public void addImage(
            String imageUrl,
            String imageKey,
            int sortOrder
    ) {
        ItemRequestImage image = new ItemRequestImage(
                this,
                imageUrl,
                imageKey,
                sortOrder
        );

        this.images.add(image);
    }

    public void removeImage(ItemRequestImage image) {
        this.images.remove(image);
    }
}