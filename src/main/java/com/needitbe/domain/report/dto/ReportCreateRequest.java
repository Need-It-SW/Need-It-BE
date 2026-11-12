package com.needitbe.domain.report.dto;

import com.needitbe.domain.report.entity.ReportReason;
import com.needitbe.domain.report.entity.ReportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(

        @NotNull(message = "신고 대상을 선택해주세요.")
        ReportType reportType,

        @NotNull(message = "신고 대상 ID가 필요합니다.")
        Long targetId,

        @NotNull(message = "신고 사유를 선택해주세요.")
        ReportReason reason,

        @Size(max = 1000, message = "상세 사유는 1000자 이내로 입력해주세요.")
        String description

) {
}