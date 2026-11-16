package com.needitbe.domain.ads.controller;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.ads.dto.AdEventRequest;
import com.needitbe.domain.ads.service.AdService;
import com.needitbe.domain.ads.service.AdEventService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;
    private final AdEventService adEventService;

    // 사용자 광고 조회
    @Operation(summary = "사용자 광고 조회")
    @GetMapping
    public ApiResponse<List<AdResponse>> getAds(
            @RequestParam String placement
    ) {

        return ApiResponse.success(
                "광고 조회에 성공했습니다.",
                adService.getAds(placement)
        );
    }

    // 광고 노출 기록
    @Operation(summary = "광고 노출 기록")
    @PostMapping("/impressions")
    public ApiResponse<Void> recordImpression(
            @Valid @RequestBody AdEventRequest request
    ) {

        adEventService.recordImpression(request);

        return ApiResponse.success(
                "광고 노출 기록에 성공했습니다.",
                null
        );
    }

    // 광고 클릭 기록
    @Operation(summary = "광고 클릭 기록")
    @PostMapping("/clicks")
    public ApiResponse<Void> recordClick(
            @Valid @RequestBody AdEventRequest request
    ) {

        adEventService.recordClick(request);

        return ApiResponse.success(
                "광고 클릭 기록에 성공했습니다.",
                null
        );
    }
}