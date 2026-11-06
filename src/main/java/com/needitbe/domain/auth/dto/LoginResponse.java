package com.needitbe.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long memberId,
        String username,
        String nickname,
        String role
) {
}