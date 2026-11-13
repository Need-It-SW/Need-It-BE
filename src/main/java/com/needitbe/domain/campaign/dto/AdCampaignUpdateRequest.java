package com.needitbe.domain.campaign.dto;

import com.needitbe.domain.campaign.entity.AdType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record AdCampaignUpdateRequest(

        @NotBlank(message = "캠페인 이름은 필수입니다.")
        @Size(max = 100)
        String name,

        @NotNull(message = "광고 노출 방식은 필수입니다.")
        AdType adType,

        String targetCategory,
        String targetAction,
        String targetKeywords,
        String targetRegions,

        @Min(value = 1, message = "최대 노출 횟수는 1 이상이어야 합니다.")
        int maxImpressions,

        @NotBlank(message = "노출 횟수 집계 기간은 필수입니다.")
        String impressionPeriod,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        String description,

        boolean active

) {
    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return !endDate.isBefore(startDate);
    }
}