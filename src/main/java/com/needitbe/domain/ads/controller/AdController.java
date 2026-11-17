package com.needitbe.domain.ads.controller;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.ads.dto.AdEventRequest;
import com.needitbe.domain.ads.service.AdService;
import com.needitbe.domain.ads.service.AdEventService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;
    private final AdEventService adEventService;

    // 사용자 광고 조회
    @Operation(summary = "사용자 광고 조회 (로그인 시 개인화 광고 제공)")
    @GetMapping
    public ApiResponse<List<AdResponse>> getAds(
            @RequestParam String placement,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return ApiResponse.success(
                    "일반 광고 조회에 성공했습니다.",
                    adService.getAds(placement)
            );
        }

        Long memberId = Long.valueOf(authentication.getName());

        return ApiResponse.success(
                "개인화 광고 조회에 성공했습니다.",
                adService.getPersonalizedAds(memberId, placement)
        );
    }

    // 광고 노출 기록
    @Operation(summary = "광고 노출 기록")
    @PostMapping("/impressions")
    public ApiResponse<Void> recordImpression(
            @Valid @RequestBody AdEventRequest request,
            Authentication authentication
    ) {

        Long memberId = getMemberId(authentication);

        adEventService.recordImpression(memberId, request);

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

    // JWT에서 회원 ID 추출
    private Long getMemberId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return null;
        }

        return Long.valueOf(authentication.getName());
    }
}