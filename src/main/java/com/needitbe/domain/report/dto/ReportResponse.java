package com.needitbe.domain.report.dto;

import com.needitbe.domain.report.entity.Report;
import com.needitbe.domain.report.entity.ReportReason;
import com.needitbe.domain.report.entity.ReportStatus;
import com.needitbe.domain.report.entity.ReportType;

import java.time.LocalDateTime;

public record ReportResponse(

        Long reportId,
        Long reporterId,
        ReportType reportType,
        Long targetId,
        ReportReason reason,
        String description,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt

) {

    public static ReportResponse from(Report report) {

        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReportType(),
                report.getTargetId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getProcessedAt()
        );
    }
}