package com.needitbe.domain.ads.controller;

import com.needitbe.domain.ads.dto.AdBannerStatisticsResponse;
import com.needitbe.domain.ads.service.AdStatisticsService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdBannerStatisticsController {

    private final AdStatisticsService adStatisticsService;

    @Operation(
            summary = "관리자 배너별 광고 통계 조회",
            description = "특정 배너의 전체 노출 수, 클릭 수, CTR을 조회합니다."
    )
    @GetMapping("/{bannerId}/statistics")
    public ApiResponse<AdBannerStatisticsResponse> getBannerStatistics(
            @PathVariable Long bannerId
    ) {

        return ApiResponse.success(
                "배너 광고 통계 조회에 성공했습니다.",
                adStatisticsService.getBannerStatistics(bannerId)
        );
    }
}