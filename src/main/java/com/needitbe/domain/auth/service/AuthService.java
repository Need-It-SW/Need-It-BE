package com.needitbe.domain.auth.service;

import com.needitbe.domain.auth.dto.*;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.needitbe.domain.member.entity.MemberStatus;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // 회원가입
    @Transactional
    public Long signup(SignupRequest request) {

        if (memberRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        Member member = new Member(
                request.username(),
                encodedPassword,
                request.nickname(),
                request.region()
        );

        return memberRepository.save(member).getId();
    }

    // 로그인
    @Transactional
    public LoginResponse login(LoginRequest request) {

        Member member = memberRepository
                .findByUsername(request.username())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_CREDENTIALS)
                );

        if (!passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 이용 정지 회원 로그인 차단
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.MEMBER_SUSPENDED);
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(member);

        String refreshToken =
                jwtTokenProvider.createRefreshToken(member);

        refreshTokenService.save(member, refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                member.getId(),
                member.getUsername(),
                member.getNickname(),
                member.getRole().name()
        );
    }

    // 토큰 재발급
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {

        Long memberId = refreshTokenService
                .validateAndGetMemberId(request.refreshToken());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // 이용 정지 회원 토큰 재발급 차단
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.MEMBER_SUSPENDED);
        }

        String newAccessToken =
                jwtTokenProvider.createAccessToken(member);

        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(member);

        refreshTokenService.save(member, newRefreshToken);

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer"
        );
    }

    // 로그아웃
    @Transactional
    public void logout(Long memberId) {
        refreshTokenService.delete(memberId);
    }
}