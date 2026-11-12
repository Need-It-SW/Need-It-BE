package com.needitbe.domain.report.dto;

import com.needitbe.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportStatusUpdateRequest(

        @NotNull(message = "변경할 신고 상태를 선택해주세요.")
        ReportStatus status

) {
}