package com.needitbe.domain.auth.controller;

import com.needitbe.domain.auth.dto.*;
import com.needitbe.domain.auth.service.AuthService;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MemberRepository memberRepository;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        Long memberId = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "회원가입이 완료되었습니다.",
                        memberId
                ));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(
            @RequestParam String username
    ) {
        boolean available = !memberRepository.existsByUsername(username);

        return ResponseEntity.ok(
                ApiResponse.success("아이디 중복 확인 완료", available)
        );
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(
            @RequestParam String nickname
    ) {
        boolean available = !memberRepository.existsByNickname(nickname);

        return ResponseEntity.ok(
                ApiResponse.success("닉네임 중복 확인 완료", available)
        );
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그인에 성공했습니다.",
                        response
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "토큰 재발급에 성공했습니다.",
                        authService.refresh(request)
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        authService.logout(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그아웃에 성공했습니다.",
                        null
                )
        );
    }
}