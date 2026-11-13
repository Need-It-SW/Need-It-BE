package com.needitbe.domain.admin.controller;

import com.needitbe.domain.admin.dto.MemberSanctionRequest;
import com.needitbe.domain.admin.dto.MemberSanctionResponse;
import com.needitbe.domain.admin.service.AdminMemberService;
import com.needitbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.needitbe.domain.admin.dto.AdminMemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    // 회원 이용 정지
    @PostMapping("/{memberId}/sanctions")
    public ResponseEntity<ApiResponse<Void>> suspendMember(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberSanctionRequest request,
            Authentication authentication
    ) {

        Long adminId = Long.valueOf(authentication.getName());

        adminMemberService.suspendMember(
                adminId,
                memberId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 이용 정지에 성공했습니다.",
                        null
                )
        );
    }

    // 회원 이용 정지 해제
    @PatchMapping("/{memberId}/sanctions/release")
    public ResponseEntity<ApiResponse<Void>> releaseMember(
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        Long adminId = Long.valueOf(authentication.getName());

        adminMemberService.releaseMember(adminId, memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 이용 정지 해제에 성공했습니다.",
                        null
                )
        );
    }

    // 회원별 제재 이력 조회
    @GetMapping("/{memberId}/sanctions")
    public ResponseEntity<ApiResponse<List<MemberSanctionResponse>>> getMemberSanctions(
            @PathVariable Long memberId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 제재 이력 조회에 성공했습니다.",
                        adminMemberService.getMemberSanctions(memberId)
                )
        );
    }

    @GetMapping
    public ApiResponse<Page<AdminMemberResponse>> getMembers(
            @RequestParam(required = false) String keyword,
            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ApiResponse.success(
                "관리자 회원 목록 조회에 성공했습니다.",
                adminMemberService.getMembers(keyword, pageable)
        );
    }

    @GetMapping("/{memberId}")
    public ApiResponse<AdminMemberResponse> getMember(
            @PathVariable Long memberId
    ) {
        return ApiResponse.success(
                "관리자 회원 상세 조회에 성공했습니다.",
                adminMemberService.getMember(memberId)
        );
    }
}