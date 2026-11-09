package com.needitbe.domain.offer.controller;

import com.needitbe.domain.offer.dto.OfferImageResponse;
import com.needitbe.domain.offer.service.OfferService;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/offers")
public class OfferImageController {

    private final OfferService offerService;

    // =====================================================
    // 나눔 제안 이미지 업로드
    // =====================================================
    @PostMapping(
            value = "/{offerId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<OfferImageResponse>>> uploadImages(
            Authentication authentication,
            @PathVariable("offerId") Long offerId,
            @RequestPart("files") List<MultipartFile> files
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<OfferImageResponse> response =
                offerService.uploadOfferImages(
                        memberId,
                        offerId,
                        files
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 제안 이미지 업로드에 성공했습니다.",
                        response
                )
        );
    }
}