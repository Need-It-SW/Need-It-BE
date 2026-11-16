package com.needitbe.domain.ads.controller;

import com.needitbe.domain.ads.dto.AdStatsResponse;
import com.needitbe.domain.ads.service.AdStatsService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class AdStatsController {

    private final AdStatsService adStatsService;

    @Operation(summary = "관리자 광고 캠페인 통계 조회")
    @GetMapping("/{campaignId}/stats")
    public ApiResponse<AdStatsResponse> getCampaignStats(
            @PathVariable Long campaignId
    ) {

        return ApiResponse.success(
                "광고 캠페인 통계 조회에 성공했습니다.",
                adStatsService.getCampaignStats(campaignId)
        );
    }
}