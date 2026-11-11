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
import com.needitbe.global.s3.S3Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.needitbe.domain.member.dto.UsernameUpdateRequest;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final S3Service s3Service;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {

        Member member = findMember(memberId);

        return toResponse(member);
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

    // 프로필 사진 변경
    @Transactional
    public MemberResponse updateProfileImage(
            Long memberId,
            MultipartFile file
    ) {
        Member member = findMember(memberId);

        // 기존 프로필 이미지 키
        String oldImageKey = member.getProfileImageKey();

        // 새 이미지 S3 업로드
        String newImageKey = s3Service.uploadProfileImage(
                file,
                memberId
        );

        // DB에 새 이미지 키 저장
        member.updateProfileImage(newImageKey);

        // 트랜잭션 결과에 따라 S3 파일 정리
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        // DB 변경이 성공하면 기존 이미지 삭제
                        if (oldImageKey != null) {
                            try {
                                s3Service.deleteImage(oldImageKey);
                            } catch (Exception e) {
                                // 기존 이미지 삭제 실패 시에도
                                // 이미 완료된 DB 변경은 유지
                            }
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {

                        // DB 변경이 실패하면 새로 업로드한 이미지 삭제
                        if (status != STATUS_COMMITTED) {
                            try {
                                s3Service.deleteImage(newImageKey);
                            } catch (Exception e) {
                                // 별도 로그 기록 권장
                            }
                        }
                    }
                }
        );

        return toResponse(member);
    }
    // 프로필 사진 삭제
    @Transactional
    public MemberResponse deleteProfileImage(Long memberId) {

        Member member = findMember(memberId);

        String oldImageKey = member.getProfileImageKey();

        if (oldImageKey == null) {
            return toResponse(member);
        }

        // DB에서 프로필 사진 연결 해제
        member.deleteProfileImage();

        // DB 변경이 성공한 후 S3 이미지 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            s3Service.deleteImage(oldImageKey);
                        } catch (Exception e) {
                            // 삭제 실패 로그 및 재시도 처리 권장
                        }
                    }
                }
        );

        return toResponse(member);
    }

    // 회원 응답 변환
    private MemberResponse toResponse(Member member) {

        String profileImageUrl = null;

        if (member.getProfileImageKey() != null) {
            profileImageUrl = s3Service.getPresignedUrl(
                    member.getProfileImageKey()
            );
        }

        return MemberResponse.from(
                member,
                profileImageUrl
        );
    }

    // 아이디 변경
    @Transactional
    public MemberResponse updateUsername(
            Long memberId,
            UsernameUpdateRequest request
    ) {

        // 1. 로그인한 회원 조회
        Member member = findMember(memberId);

        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(
                request.currentPassword(),
                member.getPassword()
        )) {
            throw new CustomException(
                    ErrorCode.INVALID_CURRENT_PASSWORD
            );
        }

        // 3. 기존 아이디와 동일한지 확인
        if (member.getUsername().equals(request.username())) {
            throw new IllegalArgumentException(
                    "현재 사용 중인 아이디와 동일합니다."
            );
        }

        // 4. 아이디 중복 확인
        if (memberRepository.existsByUsername(request.username())) {
            throw new CustomException(
                    ErrorCode.DUPLICATE_USERNAME
            );
        }

        // 5. 아이디 변경
        member.updateUsername(request.username());

        // 6. 변경된 회원 정보 반환
        return toResponse(member);
    }
}