package com.needitbe.global.security;

import com.needitbe.domain.member.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // Access Token 생성
    public String createAccessToken(Member member) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(member.getId().toString())
                .claim("username", member.getUsername())
                .claim("role", member.getRole().name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(accessTokenExpiration)
                ))
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Member member) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(member.getId().toString())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(refreshTokenExpiration)
                ))
                .signWith(secretKey)
                .compact();
    }

    // JWT 검증
    public Claims parseClaims(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "유효하지 않거나 만료된 JWT입니다.", e
            );
        }
    }

    // 회원 ID 추출
    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // Refresh Token 만료시간
    public Instant getRefreshTokenExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }
}