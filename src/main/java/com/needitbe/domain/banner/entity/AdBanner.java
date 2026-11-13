package com.needitbe.domain.banner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ad_banners")
public class AdBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 배너 이름
    @Column(nullable = false, length = 100)
    private String name;

    // S3 이미지 Key
    @Column(nullable = false, length = 500)
    private String imageKey;

    // 배너 클릭 시 이동할 URL
    // 광고 클릭 이동 주소 (선택)

    @Column(length = 1000)
    private String targetUrl;

    // 등록일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 배너 활성화 여부
    @Column(nullable = false)
    private boolean active = true;

    public AdBanner(
            String name,
            String imageKey,
            String targetUrl,
            boolean active
    ) {
        this.name = name;
        this.imageKey = imageKey;
        this.targetUrl = targetUrl;
        this.active = active;
    }

    // 배너 수정
    public void update(
            String name,
            String imageKey,
            String targetUrl,
            boolean active
    ) {
        this.name = name;
        this.imageKey = imageKey;
        this.targetUrl = targetUrl;
        this.active = active;
    }
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}