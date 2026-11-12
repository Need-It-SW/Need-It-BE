package com.needitbe.domain.report.entity;

import com.needitbe.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_reports_status", columnList = "status"),
                @Index(name = "idx_reports_reporter", columnList = "reporter_id"),
                @Index(name = "idx_reports_type", columnList = "report_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고한 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    // 신고 대상 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    // 신고 대상 ID (회원 ID 또는 게시글 ID)
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    // 상세 사유
    @Column(length = 1000)
    private String description;

    // 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    // 신고 접수 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 관리자 처리 시간
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Report(
            Member reporter,
            ReportType reportType,
            Long targetId,
            ReportReason reason,
            String description
    ) {
        this.reporter = reporter;
        this.reportType = reportType;
        this.targetId = targetId;
        this.reason = reason;
        this.description = description;
        this.status = ReportStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // 관리자 신고 처리
    public void updateStatus(ReportStatus status) {

        this.status = status;

        if (status == ReportStatus.RESOLVED
                || status == ReportStatus.REJECTED) {

            this.processedAt = LocalDateTime.now();

        } else {
            this.processedAt = null;
        }
    }
}