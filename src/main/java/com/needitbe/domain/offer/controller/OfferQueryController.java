package com.needitbe.domain.offer.controller;

import com.needitbe.domain.offer.dto.OfferSentResponse;
import com.needitbe.domain.offer.service.OfferService;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.needitbe.domain.offer.dto.OfferReceivedResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/offers")
public class OfferQueryController {

    private final OfferService offerService;

    // =====================================================
    // 내가 보낸 제안 목록 조회
    // =====================================================
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<List<OfferSentResponse>>> getSentOffers(
            Authentication authentication
    ) {

        // JWT에서 로그인 회원 ID 추출
        Long memberId = Long.valueOf(authentication.getName());

        // 보낸 제안 조회
        List<OfferSentResponse> response =
                offerService.getSentOffers(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "보낸 제안 목록 조회에 성공했습니다.",
                        response
                )
        );
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<OfferReceivedResponse>>> getReceivedOffers(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<OfferReceivedResponse> response =
                offerService.getReceivedOffers(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "받은 제안 목록 조회에 성공했습니다.",
                        response
                )
        );
    }
}