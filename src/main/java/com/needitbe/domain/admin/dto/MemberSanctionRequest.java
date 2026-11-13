package com.needitbe.domain.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberSanctionRequest(

        @NotBlank(message = "제재 사유를 입력해주세요.")
        @Size(max = 500)
        String reason,

        @Min(value = 1, message = "제재 기간은 최소 1일 이상이어야 합니다.")
        Integer durationDays
) {
}