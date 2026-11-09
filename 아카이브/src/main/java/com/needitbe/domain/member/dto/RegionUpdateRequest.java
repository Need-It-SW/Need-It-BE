package com.needitbe.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionUpdateRequest(

        @NotBlank(message = "활동 동네를 선택해주세요.")
        @Size(max = 100,
                message = "활동 동네는 100자 이하여야 합니다.")
        String region

) {
}