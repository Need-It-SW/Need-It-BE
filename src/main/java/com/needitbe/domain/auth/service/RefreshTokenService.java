package com.needitbe.domain.auth.service;

import com.needitbe.domain.auth.entity.RefreshToken;
import com.needitbe.domain.auth.repository.RefreshTokenRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.global.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // Refresh Token을 SHA-256 해시로 변환
    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.", e
            );
        }
    }

    // Refresh Token 저장 또는 갱신
    @Transactional
    public void save(Member member, String token) {

        String tokenHash = hashToken(token);

        Instant expiresAt =
                jwtTokenProvider.getRefreshTokenExpiration(token);

        RefreshToken refreshToken =
                refreshTokenRepository.findByMemberId(member.getId())
                        .orElse(null);

        if (refreshToken == null) {

            refreshTokenRepository.save(
                    new RefreshToken(
                            member,
                            tokenHash,
                            expiresAt
                    )
            );

        } else {
            refreshToken.update(tokenHash, expiresAt);
        }
    }

    // Refresh Token 검증
    @Transactional(readOnly = true)
    public Long validateAndGetMemberId(String token) {

        try {
            Claims claims = jwtTokenProvider.parseClaims(token);

            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            Long memberId = Long.valueOf(claims.getSubject());

            RefreshToken savedToken =
                    refreshTokenRepository.findByMemberId(memberId)
                            .orElseThrow(() ->
                                    new CustomException(ErrorCode.INVALID_TOKEN)
                            );

            if (savedToken.getExpiresAt().isBefore(Instant.now())) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            String requestHash = hashToken(token);

            boolean matches = MessageDigest.isEqual(
                    savedToken.getTokenHash().getBytes(StandardCharsets.UTF_8),
                    requestHash.getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }

            return memberId;

        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    // 로그아웃
    @Transactional
    public void delete(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}