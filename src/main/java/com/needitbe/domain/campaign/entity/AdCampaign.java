package com.needitbe.domain.campaign.entity;
import com.needitbe.domain.campaign.dto.AdCampaignUpdateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.needitbe.domain.campaign.dto.AdCampaignCreateRequest;

@Entity
@Table(name = "ad_campaigns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 캠페인 이름
    @Column(nullable = false, length = 100)
    private String name;

    // 일반 광고 / 맞춤형 광고
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdType adType;

    // 타깃 카테고리
    @Column(length = 50)
    private String targetCategory;

    // 수집 행동 유형
    @Column(length = 50)
    private String targetAction;

    // 타깃 키워드 (쉼표 구분)
    @Column(columnDefinition = "TEXT")
    private String targetKeywords;

    // 대상 지역 (쉼표 구분)
    @Column(columnDefinition = "TEXT")
    private String targetRegions;

    // 사용자별 최대 노출 횟수
    @Column(nullable = false)
    private int maxImpressions;

    // 노출 횟수 집계 기간
    @Column(nullable = false, length = 30)
    private String impressionPeriod;

    // 시작일
    @Column(nullable = false)
    private LocalDate startDate;

    // 종료일
    @Column(nullable = false)
    private LocalDate endDate;

    // 캠페인 설명
    @Column(columnDefinition = "TEXT")
    private String description;

    // 활성화 여부
    @Column(nullable = false)
    private boolean active = true;

    // 생성일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정일
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 캠페인 상태 계산
    @Transient
    public CampaignStatus getStatus() {

        LocalDate today = LocalDate.now();

        if (today.isAfter(endDate)) {
            return CampaignStatus.ENDED;
        }

        if (!active) {
            return CampaignStatus.PAUSED;
        }

        if (today.isBefore(startDate)) {
            return CampaignStatus.SCHEDULED;
        }

        return CampaignStatus.RUNNING;
    }



    // 캠페인 생성자
    public AdCampaign(AdCampaignCreateRequest request) {

        this.name = request.name().trim();
        this.adType = request.adType();

        this.targetCategory = request.targetCategory();
        this.targetAction = request.targetAction();
        this.targetKeywords = request.targetKeywords();
        this.targetRegions = request.targetRegions();

        this.maxImpressions = request.maxImpressions();
        this.impressionPeriod = request.impressionPeriod();

        this.startDate = request.startDate();
        this.endDate = request.endDate();

        this.description = request.description();
        this.active = request.active();
    }

    public void update(AdCampaignUpdateRequest request) {

        this.name = request.name().trim();
        this.adType = request.adType();

        this.targetCategory = request.targetCategory();
        this.targetAction = request.targetAction();
        this.targetKeywords = request.targetKeywords();
        this.targetRegions = request.targetRegions();

        this.maxImpressions = request.maxImpressions();
        this.impressionPeriod = request.impressionPeriod();

        this.startDate = request.startDate();
        this.endDate = request.endDate();

        this.description = request.description();
        this.active = request.active();
    }

}