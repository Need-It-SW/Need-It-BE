package com.needitbe.domain.campaign.controller;

import com.needitbe.domain.campaign.dto.AdCampaignCreateRequest;
import com.needitbe.domain.campaign.dto.AdCampaignResponse;
import com.needitbe.domain.campaign.dto.AdCampaignUpdateRequest;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.service.AdCampaignService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class AdCampaignController {

    private final AdCampaignService adCampaignService;

    // 캠페인 등록
    @Operation(summary = "관리자 광고 캠페인 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<AdCampaignResponse>> createCampaign(
            @Valid @RequestBody AdCampaignCreateRequest request
    ) {

        AdCampaignResponse response =
                adCampaignService.createCampaign(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "광고 캠페인 등록에 성공했습니다.",
                        response
                ));
    }

    // 캠페인 목록 조회 및 검색
    @Operation(summary = "관리자 광고 캠페인 목록 조회 및 검색")
    @GetMapping
    public ApiResponse<List<AdCampaignResponse>> getCampaigns(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CampaignStatus status
    ) {

        return ApiResponse.success(
                "광고 캠페인 목록 조회에 성공했습니다.",
                adCampaignService.getCampaigns(keyword, status)
        );
    }

    // 캠페인 상세 조회
    @Operation(summary = "관리자 광고 캠페인 상세 조회")
    @GetMapping("/{campaignId}")
    public ApiResponse<AdCampaignResponse> getCampaign(
            @PathVariable Long campaignId
    ) {

        return ApiResponse.success(
                "광고 캠페인 상세 조회에 성공했습니다.",
                adCampaignService.getCampaign(campaignId)
        );
    }

    // 캠페인 수정
    @Operation(summary = "관리자 광고 캠페인 수정")
    @PatchMapping("/{campaignId}")
    public ApiResponse<AdCampaignResponse> updateCampaign(
            @PathVariable Long campaignId,
            @Valid @RequestBody AdCampaignUpdateRequest request
    ) {

        return ApiResponse.success(
                "광고 캠페인 수정에 성공했습니다.",
                adCampaignService.updateCampaign(campaignId, request)
        );
    }

    // 캠페인 삭제
    @Operation(summary = "관리자 광고 캠페인 삭제")
    @DeleteMapping("/{campaignId}")
    public ApiResponse<Void> deleteCampaign(
            @PathVariable Long campaignId
    ) {

        adCampaignService.deleteCampaign(campaignId);

        return ApiResponse.success(
                "광고 캠페인 삭제에 성공했습니다.",
                null
        );
    }
}