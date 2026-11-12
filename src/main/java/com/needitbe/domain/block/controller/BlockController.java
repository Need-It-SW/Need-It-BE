package com.needitbe.domain.block.controller;

import com.needitbe.domain.block.dto.BlockResponse;
import com.needitbe.domain.block.service.BlockService;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    // 1. 회원 차단
    @PostMapping("/{memberId}")
    public ResponseEntity<ApiResponse<BlockResponse>> blockMember(
            @PathVariable Long memberId,
            Authentication authentication
    ) {

        Long blockerId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 차단에 성공했습니다.",
                        blockService.blockMember(
                                blockerId,
                                memberId
                        )
                )
        );
    }

    // 2. 회원 차단 해제
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> unblockMember(
            @PathVariable Long memberId,
            Authentication authentication
    ) {

        Long blockerId = Long.valueOf(authentication.getName());

        blockService.unblockMember(blockerId, memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 차단을 해제했습니다.",
                        null
                )
        );
    }

    // 3. 내가 차단한 회원 목록
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BlockResponse>>> getMyBlocks(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "차단 회원 목록 조회에 성공했습니다.",
                        blockService.getMyBlocks(memberId)
                )
        );
    }
}