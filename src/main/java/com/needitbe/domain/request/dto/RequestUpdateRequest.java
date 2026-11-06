package com.needitbe.domain.request.dto;

import com.needitbe.domain.request.entity.RequestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestUpdateRequest(

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100)
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String description,

        @NotNull(message = "카테고리를 선택해주세요.")
        RequestCategory category,

        @NotBlank(message = "지역을 입력해주세요.")
        @Size(max = 100)
        String region
) {
}