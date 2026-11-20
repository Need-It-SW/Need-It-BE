package com.needitbe.domain.useraction.controller;

import com.needitbe.domain.useraction.dto.CategoryClickRequest;
import com.needitbe.domain.useraction.service.UserActionService;
import com.needitbe.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-actions")
public class UserActionController {

    private final UserActionService userActionService;

    @PostMapping("/category-click")
    public ResponseEntity<ApiResponse<Void>> recordCategoryClick(
            Authentication authentication,
            @Valid @RequestBody CategoryClickRequest request
    ) {

        Long memberId =
                Long.valueOf(authentication.getName());

        userActionService.recordCategoryClick(
                memberId,
                request.category()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "카테고리 클릭 행동 기록을 요청했습니다.",
                        null
                )
        );
    }
}