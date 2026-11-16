package com.needitbe.domain.ads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UUID;

public record AdEventRequest(

        @NotNull
        @Positive
        Long campaignId,

        @NotNull
        @Positive
        Long bannerId,

        @NotBlank
        @UUID
        String impressionId

) {
}