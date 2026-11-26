package com.needitbe.domain.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_target_dismissals")
@Getter
@NoArgsConstructor
public class ReportTargetDismissal {
    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "last_report_id", nullable = false)
    private Long lastReportId;

    public ReportTargetDismissal(Long memberId, Long lastReportId) {
        this.memberId = memberId;
        this.lastReportId = lastReportId;
    }

    public void updateLastReportId(Long reportId) { this.lastReportId = reportId; }
}
