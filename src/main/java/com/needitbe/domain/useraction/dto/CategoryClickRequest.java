package com.needitbe.domain.useraction.dto;

import com.needitbe.domain.request.entity.RequestCategory;
import jakarta.validation.constraints.NotNull;

public record CategoryClickRequest(
        @NotNull RequestCategory category
) {
}