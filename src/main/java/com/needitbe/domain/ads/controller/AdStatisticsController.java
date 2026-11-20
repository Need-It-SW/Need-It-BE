package com.needitbe.domain.ads.controller;

import com.needitbe.domain.ads.dto.AdStatisticsResponse;
import com.needitbe.domain.ads.service.AdStatisticsService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class AdStatisticsController {

    private final AdStatisticsService adStatisticsService;

    @Operation(
            summary = "관리자 캠페인별 광고 통계 조회",
            description = "캠페인의 전체 노출 수, 클릭 수, 클릭률(CTR)을 조회합니다."
    )
    @GetMapping("/{campaignId}/statistics")
    public ApiResponse<AdStatisticsResponse> getCampaignStatistics(
            @PathVariable Long campaignId
    ) {

        return ApiResponse.success(
                "광고 통계 조회에 성공했습니다.",
                adStatisticsService.getCampaignStatistics(campaignId)
        );
    }

    @Operation(
            summary = "관리자 전체 캠페인 광고 통계 조회",
            description = "전체 캠페인의 노출 수, 클릭 수, CTR을 조회합니다."
    )
    @GetMapping("/statistics")
    public ApiResponse<List<AdStatisticsResponse>> getAllCampaignStatistics() {

        return ApiResponse.success(
                "전체 광고 통계 조회에 성공했습니다.",
                adStatisticsService.getAllCampaignStatistics()
        );
    }

    @Operation(
            summary = "관리자 캠페인 기간별 광고 통계 조회",
            description = "특정 기간의 광고 노출 수, 클릭 수, CTR을 조회합니다."
    )
    @GetMapping("/{campaignId}/statistics/period")
    public ApiResponse<AdStatisticsResponse> getCampaignStatisticsByPeriod(
            @PathVariable Long campaignId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {

        return ApiResponse.success(
                "기간별 광고 통계 조회에 성공했습니다.",
                adStatisticsService.getCampaignStatisticsByPeriod(
                        campaignId,
                        start,
                        end
                )
        );
    }

    @Operation(
            summary = "관리자 전체 캠페인 기간별 광고 통계 조회",
            description = "지정한 기간의 모든 캠페인별 노출 수, 클릭 수, CTR을 조회합니다."
    )
    @GetMapping("/statistics/period")
    public ApiResponse<List<AdStatisticsResponse>> getAllCampaignStatisticsByPeriod(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end
    ) {

        return ApiResponse.success(
                "전체 캠페인 기간별 광고 통계 조회에 성공했습니다.",
                adStatisticsService.getAllCampaignStatisticsByPeriod(
                        start,
                        end
                )
        );
    }
}