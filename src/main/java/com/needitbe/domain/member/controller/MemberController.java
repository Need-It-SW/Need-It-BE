package com.needitbe.domain.member.controller;

import com.needitbe.domain.member.dto.MemberResponse;
import com.needitbe.domain.member.dto.NicknameUpdateRequest;
import com.needitbe.domain.member.dto.PasswordUpdateRequest;
import com.needitbe.domain.member.dto.RegionUpdateRequest;
import com.needitbe.domain.member.service.MemberService;
import com.needitbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.needitbe.domain.member.dto.UsernameUpdateRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원정보 조회에 성공했습니다.",
                        memberService.getMyInfo(memberId)
                )
        );
    }

    // 닉네임 변경
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<MemberResponse>> updateNickname(
            Authentication authentication,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "닉네임 변경에 성공했습니다.",
                        memberService.updateNickname(memberId, request)
                )
        );
    }

    // 활동 동네 변경
    @PatchMapping("/me/region")
    public ResponseEntity<ApiResponse<MemberResponse>> updateRegion(
            Authentication authentication,
            @Valid @RequestBody RegionUpdateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "활동 동네 변경에 성공했습니다.",
                        memberService.updateRegion(memberId, request)
                )
        );
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordUpdateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        memberService.updatePassword(memberId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "비밀번호 변경에 성공했습니다.",
                        null
                )
        );
    }

    // 프로필 사진 변경
    @PatchMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfileImage(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "프로필 사진 변경에 성공했습니다.",
                        memberService.updateProfileImage(memberId, file)
                )
        );
    }

    // 프로필 사진 삭제
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<MemberResponse>> deleteProfileImage(
            Authentication authentication
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "프로필 사진 삭제에 성공했습니다.",
                        memberService.deleteProfileImage(memberId)
                )
        );
    }
    // 아이디 변경
    @PatchMapping("/me/username")
    public ResponseEntity<ApiResponse<MemberResponse>> updateUsername(
            Authentication authentication,
            @Valid @RequestBody UsernameUpdateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "아이디 변경에 성공했습니다.",
                        memberService.updateUsername(memberId, request)
                )
        );
    }


}