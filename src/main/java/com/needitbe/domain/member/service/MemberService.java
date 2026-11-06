package com.needitbe.domain.member.service;

import com.needitbe.domain.auth.service.RefreshTokenService;
import com.needitbe.domain.member.dto.MemberResponse;
import com.needitbe.domain.member.dto.NicknameUpdateRequest;
import com.needitbe.domain.member.dto.PasswordUpdateRequest;
import com.needitbe.domain.member.dto.RegionUpdateRequest;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {

        Member member = findMember(memberId);

        return MemberResponse.from(member);
    }

    // 닉네임 변경
    @Transactional
    public MemberResponse updateNickname(
            Long memberId,
            NicknameUpdateRequest request
    ) {
        Member member = findMember(memberId);

        // 1. 기존 닉네임과 동일한지 확인
        if (member.getNickname().equals(request.nickname())) {
            throw new CustomException(ErrorCode.SAME_NICKNAME);
        }

        // 2. 다른 회원이 사용 중인 닉네임인지 확인
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 3. 닉네임 변경
        member.updateNickname(request.nickname());

        return MemberResponse.from(member);
    }

    // 활동 동네 변경
    @Transactional
    public MemberResponse updateRegion(
            Long memberId,
            RegionUpdateRequest request
    ) {
        Member member = findMember(memberId);

        // 기존 활동 동네와 변경하려는 동네 비교
        if (member.getRegion().equals(request.region())) {
            throw new CustomException(ErrorCode.SAME_REGION);
        }

        member.updateRegion(request.region());

        return MemberResponse.from(member);
    }

    // 공통 회원 조회
    private Member findMember(Long memberId) {

        return memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEMBER_NOT_FOUND
                        )
                );
    }
    @Transactional
    public void updatePassword(
            Long memberId,
            PasswordUpdateRequest request
    ) {

        // 1. 로그인한 회원 조회
        Member member = findMember(memberId);

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(
                request.currentPassword(),
                member.getPassword()
        )) {
            throw new CustomException(
                    ErrorCode.INVALID_CURRENT_PASSWORD
            );
        }

        // 3. 새 비밀번호 확인
        if (!request.newPassword().equals(
                request.newPasswordConfirm()
        )) {
            throw new CustomException(
                    ErrorCode.NEW_PASSWORD_MISMATCH
            );
        }

        // 4. 기존 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(
                request.newPassword(),
                member.getPassword()
        )) {
            throw new CustomException(
                    ErrorCode.SAME_PASSWORD
            );
        }

        // 5. 새 비밀번호 암호화
        String encodedPassword =
                passwordEncoder.encode(request.newPassword());

        // 6. 비밀번호 변경
        member.updatePassword(encodedPassword);

        // 7. 기존 Refresh Token 무효화
        refreshTokenService.delete(memberId);
    }
}