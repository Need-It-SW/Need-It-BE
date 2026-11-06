package com.needitbe.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}