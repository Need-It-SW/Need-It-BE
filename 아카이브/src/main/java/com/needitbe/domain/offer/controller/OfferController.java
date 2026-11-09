package com.needitbe.domain.offer.controller;

import com.needitbe.domain.offer.dto.OfferCreateRequest;
import com.needitbe.domain.offer.dto.OfferResponse;
import com.needitbe.domain.offer.service.OfferService;
import com.needitbe.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests/{requestId}/offers")
public class OfferController {

    private final OfferService offerService;

    // =====================================================
    // 물품 제공 제안 등록
    // =====================================================
    @PostMapping
    public ResponseEntity<ApiResponse<OfferResponse>> createOffer(
            Authentication authentication,
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody OfferCreateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        OfferResponse response = offerService.createOffer(
                memberId,
                requestId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "물품 제공 제안이 등록되었습니다.",
                                response
                        )
                );
    }

    @DeleteMapping("/{offerId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            Authentication authentication,
            @PathVariable("offerId") Long offerId,
            @PathVariable("imageId") Long imageId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        offerService.deleteOfferImage(
                memberId,
                offerId,
                imageId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 제안 이미지가 삭제되었습니다.",
                        null
                )
        );
    }
}