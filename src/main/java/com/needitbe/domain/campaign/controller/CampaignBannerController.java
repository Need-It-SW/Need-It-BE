package com.needitbe.domain.campaign.controller;

import com.needitbe.domain.campaign.dto.CampaignBannerCreateRequest;
import com.needitbe.domain.campaign.dto.CampaignBannerResponse;
import com.needitbe.domain.campaign.service.CampaignBannerService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class CampaignBannerController {

    private final CampaignBannerService campaignBannerService;

    // 캠페인에 배너 연결
    @Operation(summary = "관리자 캠페인 광고 배너 연결")
    @PostMapping("/{campaignId}/banners")
    public ApiResponse<CampaignBannerResponse> addBanner(
            @PathVariable Long campaignId,
            @Valid @RequestBody CampaignBannerCreateRequest request
    ) {

        return ApiResponse.success(
                "캠페인에 광고 배너 연결을 완료했습니다.",
                campaignBannerService.addBanner(campaignId, request)
        );
    }

    // 캠페인에 연결된 배너 목록 조회
    @Operation(summary = "관리자 캠페인에 연결된 광고 배너 목록 조회")
    @GetMapping("/{campaignId}/banners")
    public ApiResponse<List<CampaignBannerResponse>> getCampaignBanners(
            @PathVariable Long campaignId
    ) {

        return ApiResponse.success(
                "캠페인에 연결된 광고 배너 목록 조회에 성공했습니다.",
                campaignBannerService.getCampaignBanners(campaignId)
        );
    }

    // 캠페인 배너 연결 해제
    @Operation(summary = "관리자 캠페인 광고 배너 연결 해제")
    @DeleteMapping("/{campaignId}/banners/{campaignBannerId}")
    public ApiResponse<Void> removeBanner(
            @PathVariable Long campaignId,
            @PathVariable Long campaignBannerId
    ) {

        campaignBannerService.removeBanner(
                campaignId,
                campaignBannerId
        );

        return ApiResponse.success(
                "캠페인 광고 배너 연결이 해제되었습니다.",
                null
        );
    }
}