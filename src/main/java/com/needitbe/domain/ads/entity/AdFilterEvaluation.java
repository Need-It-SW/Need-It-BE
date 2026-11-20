package com.needitbe.domain.ads.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ad_filter_evaluations",
        indexes = {
                @Index(
                        name = "idx_filter_campaign_created",
                        columnList = "campaign_id, created_at"
                ),
                @Index(
                        name = "idx_filter_member",
                        columnList = "member_id"
                )
        }
)
public class AdFilterEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 평가 대상 캠페인
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    // 평가 대상 회원
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 광고 후보 선정 / 제외
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdFilterResult result;

    // 평가 발생 시간
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    // 평가에 포함된 제외 사유
    @OneToMany(
            mappedBy = "evaluation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AdFilterExclusionReason> exclusionReasons =
            new ArrayList<>();

    public AdFilterEvaluation(
            Long campaignId,
            Long memberId,
            AdFilterResult result
    ) {
        this.campaignId = campaignId;
        this.memberId = memberId;
        this.result = result;
    }

    // 제외 사유 추가
    public void addExclusionReason(
            AdExclusionReasonType reason,
            String detail
    ) {

        if (result != AdFilterResult.EXCLUDED) {
            throw new IllegalStateException(
                    "제외된 광고 후보에만 제외 사유를 추가할 수 있습니다."
            );
        }

        AdFilterExclusionReason exclusionReason =
                new AdFilterExclusionReason(
                        this,
                        reason,
                        detail
                );

        exclusionReasons.add(exclusionReason);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}