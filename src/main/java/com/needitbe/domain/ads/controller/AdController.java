package com.needitbe.domain.ads.controller;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.ads.service.AdService;
import com.needitbe.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

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
}