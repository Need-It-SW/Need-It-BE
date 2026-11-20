package com.needitbe.domain.ads.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ad_filter_exclusion_reasons",
        indexes = {
                @Index(
                        name = "idx_exclusion_reason",
                        columnList = "reason"
                )
        }
)
public class AdFilterExclusionReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 평가에서 제외됐는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "evaluation_id",
            nullable = false
    )
    private AdFilterEvaluation evaluation;

    // 제외 사유
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdExclusionReasonType reason;

    // 상세 설명
    @Column(length = 255)
    private String detail;

    public AdFilterExclusionReason(
            AdFilterEvaluation evaluation,
            AdExclusionReasonType reason,
            String detail
    ) {
        this.evaluation = evaluation;
        this.reason = reason;
        this.detail = detail;
    }
}