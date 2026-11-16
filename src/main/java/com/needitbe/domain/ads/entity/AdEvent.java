package com.needitbe.domain.ads.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ad_events",
        indexes = {
                @Index(
                        name = "idx_ad_event_campaign",
                        columnList = "campaign_id"
                ),
                @Index(
                        name = "idx_ad_event_created_at",
                        columnList = "created_at"
                )
        }
)
public class AdEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 광고 캠페인 ID
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    // 광고 배너 ID
    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    // 광고 이벤트 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private AdEventType eventType;

    @Column(name = "impression_id", length = 36)
    private String impressionId;

    // 이벤트 발생 시간
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )


    private LocalDateTime createdAt;

    public AdEvent(
            Long campaignId,
            Long bannerId,
            AdEventType eventType,
            String impressionId
    ) {
        this.campaignId = campaignId;
        this.bannerId = bannerId;
        this.eventType = eventType;
        this.impressionId = impressionId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}