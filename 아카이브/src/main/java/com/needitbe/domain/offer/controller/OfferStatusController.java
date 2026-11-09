package com.needitbe.domain.offer.controller;

import com.needitbe.domain.offer.dto.OfferResponse;
import com.needitbe.domain.offer.service.OfferService;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.needitbe.domain.offer.dto.OfferAcceptanceCancelRequest;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/offers")
public class OfferStatusController {

    private final OfferService offerService;

    // 제안 수락
    @PatchMapping("/{offerId}/accept")
    public ResponseEntity<ApiResponse<OfferResponse>> acceptOffer(
            Authentication authentication,
            @PathVariable Long offerId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        OfferResponse response =
                offerService.acceptOffer(memberId, offerId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 제안을 수락했습니다.",
                        response
                )
        );
    }

    // 제안 거절
    @PatchMapping("/{offerId}/reject")
    public ResponseEntity<ApiResponse<OfferResponse>> rejectOffer(
            Authentication authentication,
            @PathVariable Long offerId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        OfferResponse response =
                offerService.rejectOffer(memberId, offerId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 제안을 거절했습니다.",
                        response
                )
        );
    }

    // 제안 취소
    @PatchMapping("/{offerId}/cancel")
    public ResponseEntity<ApiResponse<OfferResponse>> cancelOffer(
            Authentication authentication,
            @PathVariable Long offerId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        OfferResponse response =
                offerService.cancelOffer(memberId, offerId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 제안을 취소했습니다.",
                        response
                )
        );
    }

    // 나눔 수락 취소
    @PatchMapping("/{offerId}/accept/cancel")
    public ResponseEntity<ApiResponse<OfferResponse>> cancelOfferAcceptance(
            Authentication authentication,
            @PathVariable Long offerId,
            @Valid @RequestBody OfferAcceptanceCancelRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        OfferResponse response =
                offerService.cancelOfferAcceptance(
                        memberId,
                        offerId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 수락을 취소했습니다.",
                        response
                )
        );
    }

    // =====================================================
// 나눔 거래 완료
// =====================================================
    @PatchMapping("/{offerId}/complete")
    public ResponseEntity<ApiResponse<OfferResponse>> completeOffer(
            Authentication authentication,
            @PathVariable Long offerId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        OfferResponse response =
                offerService.completeOffer(memberId, offerId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "나눔 거래가 완료되었습니다.",
                        response
                )
        );
    }
}