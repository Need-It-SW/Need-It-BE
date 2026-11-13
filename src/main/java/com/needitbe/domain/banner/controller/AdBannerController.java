package com.needitbe.domain.banner.controller;

import com.needitbe.domain.banner.dto.AdBannerCreateRequest;
import com.needitbe.domain.banner.dto.AdBannerResponse;
import com.needitbe.domain.banner.dto.AdBannerUpdateRequest;
import com.needitbe.domain.banner.service.AdBannerService;
import com.needitbe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class AdBannerController {

    private final AdBannerService adBannerService;

    @Operation(summary = "관리자 광고 배너 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdBannerResponse> createBanner(
            @Valid @ModelAttribute AdBannerCreateRequest request
    ) {

        AdBannerResponse response =
                adBannerService.createBanner(
                        request,
                        request.image()
                );

        return ApiResponse.success(
                "광고 배너 등록에 성공했습니다.",
                response
        );
    }

    // 광고 배너 목록 조회
    @Operation(summary = "관리자 광고 배너 목록 조회")
    @GetMapping
    public ApiResponse<List<AdBannerResponse>> getBanners() {

        return ApiResponse.success(
                "광고 배너 목록 조회에 성공했습니다.",
                adBannerService.getBanners()
        );
    }

    // 광고 배너 상세 조회
    @Operation(summary = "관리자 광고 배너 상세 조회")
    @GetMapping("/{bannerId}")
    public ApiResponse<AdBannerResponse> getBanner(
            @PathVariable Long bannerId
    ) {

        return ApiResponse.success(
                "광고 배너 상세 조회에 성공했습니다.",
                adBannerService.getBanner(bannerId)
        );
    }

    @Operation(summary = "관리자 광고 배너 수정")
    @PatchMapping(
            value = "/{bannerId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<AdBannerResponse> updateBanner(
            @PathVariable Long bannerId,
            @Valid @ModelAttribute AdBannerUpdateRequest request
    ) {
        return ApiResponse.success(
                "광고 배너 수정에 성공했습니다.",
                adBannerService.updateBanner(bannerId, request)
        );
    }
    @Operation(summary = "관리자 광고 배너 삭제")
    @DeleteMapping("/{bannerId}")
    public ApiResponse<Void> deleteBanner(
            @PathVariable Long bannerId
    ) {
        adBannerService.deleteBanner(bannerId);

        return ApiResponse.success(
                "광고 배너 삭제에 성공했습니다.",
                null
        );
    }
}