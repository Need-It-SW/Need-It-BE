package com.needitbe.domain.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfferCreateRequest(

        // 나눔할 물품 설명
        @NotBlank(message = "나눔할 물품을 설명해주세요.")
        @Size(
                max = 200,
                message = "물품 설명은 200자 이하로 입력해주세요."
        )
        String itemDescription,

        // 전달할 메시지
        @NotBlank(message = "전달할 메시지를 입력해주세요.")
        @Size(
                max = 2000,
                message = "메시지는 2000자 이하로 입력해주세요."
        )
        String message

) {
}