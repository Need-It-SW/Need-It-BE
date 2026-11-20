package com.needitbe.domain.campaign.dto;

import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.request.entity.RequestCategory;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

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
        @Pattern(
                regexp = "TOTAL|ALL|DAILY|WEEKLY|LAST_24_HOURS|LAST_7_DAYS",
                message = "지원하지 않는 노출 집계 기간입니다."
        )
        String impressionPeriod,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        String description,

        boolean active

) {

    // 시작일 / 종료일 검증
    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isValidDateRange() {

        if (startDate == null || endDate == null) {
            return true;
        }

        return !endDate.isBefore(startDate);
    }

    // 일반 광고는 개인화 타깃 설정 불가
    @AssertTrue(message = "일반 광고는 개인화 타깃을 설정할 수 없습니다.")
    public boolean isValidGeneralAd() {

        if (adType != AdType.GENERAL) {
            return true;
        }

        return (targetCategory == null || targetCategory.isBlank())
                && (targetAction == null || targetAction.isBlank())
                && (targetKeywords == null || targetKeywords.isBlank());
    }

    // 개인화 광고 타깃값 검증
    @AssertTrue(message = "개인화 광고의 타깃 카테고리 또는 행동 유형이 올바르지 않습니다.")
    public boolean isValidPersonalizedAd() {

        if (adType != AdType.PERSONALIZED) {
            return true;
        }

        if (targetCategory == null || targetCategory.isBlank()
                || targetAction == null || targetAction.isBlank()) {
            return false;
        }

        try {
            RequestCategory.valueOf(
                    targetCategory.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }

        return Set.of(
                "SEARCH",
                "CATEGORY_CLICK",
                "VIEW",
                "REQUEST_CREATE",
                "FAVORITE",
                "OFFER",
                "ALL"
        ).contains(
                targetAction.trim().toUpperCase(Locale.ROOT)
        );
    }
}