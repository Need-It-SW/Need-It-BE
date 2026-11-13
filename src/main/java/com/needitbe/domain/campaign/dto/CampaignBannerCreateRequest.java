package com.needitbe.domain.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CampaignBannerCreateRequest(

        @NotNull(message = "배너 ID는 필수입니다.")
        Long bannerId,

        @NotBlank(message = "광고 노출 영역은 필수입니다.")
        String placement

) {
}