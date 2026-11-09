package com.needitbe.domain.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfferAcceptanceCancelRequest(

        @NotBlank(message = "취소 사유를 선택해주세요.")
        @Size(max = 100, message = "취소 사유는 100자 이하여야 합니다.")
        String reason

) {
}