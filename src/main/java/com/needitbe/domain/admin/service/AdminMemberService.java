package com.needitbe.domain.admin.service;

import com.needitbe.domain.admin.dto.MemberSanctionRequest;

import com.needitbe.domain.admin.dto.MemberSanctionResponse;
import com.needitbe.domain.admin.entity.MemberSanction;

import com.needitbe.domain.admin.repository.MemberSanctionRepository;

import com.needitbe.domain.member.entity.Member;

import com.needitbe.domain.member.entity.MemberRole;

import com.needitbe.domain.member.entity.MemberStatus;
import com.needitbe.domain.member.repository.MemberRepository;

import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.needitbe.domain.admin.dto.AdminMemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

@Service

@RequiredArgsConstructor

public class AdminMemberService {

    private final MemberRepository memberRepository;

    private final MemberSanctionRepository sanctionRepository;

    @Transactional

    public void suspendMember(

            Long adminId,

            Long memberId,

            MemberSanctionRequest request

    ) {

        Member admin = memberRepository.findById(adminId)

                .orElseThrow(() ->

                        new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (admin.getRole() != MemberRole.ADMIN) {

            throw new SecurityException("관리자 권한이 없습니다.");

        }

        Member member = memberRepository.findById(memberId)

                .orElseThrow(() ->

                        new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 관리자 계정 제재 방지

        if (member.getRole() == MemberRole.ADMIN) {

            throw new IllegalArgumentException("관리자 계정은 제재할 수 없습니다.");

        }

        // 중복 제재 방지

        if (member.getStatus()

                == com.needitbe.domain.member.entity.MemberStatus.SUSPENDED) {

            throw new IllegalStateException("이미 이용 정지된 회원입니다.");

        }

        LocalDateTime expiresAt = request.durationDays() == null

                ? null

                : LocalDateTime.now().plusDays(request.durationDays());

        MemberSanction sanction = new MemberSanction(

                member,

                admin,

                request.reason(),

                expiresAt

        );

        sanctionRepository.save(sanction);

        member.suspend();

    }

    @Transactional
    public void releaseMember(Long adminId, Long memberId) {

        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (admin.getRole() != MemberRole.ADMIN) {
            throw new SecurityException("관리자 권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원을 찾을 수 없습니다."));

        MemberSanction sanction = sanctionRepository
                .findFirstByMemberIdAndReleasedAtIsNullOrderByStartedAtDesc(memberId)
                .orElseThrow(() ->
                        new IllegalStateException("해제할 제재 이력이 없습니다."));

        if (member.getStatus() != MemberStatus.SUSPENDED) {
            throw new IllegalStateException("이용 정지 상태가 아닌 회원입니다.");
        }

        sanction.release();
        member.activate();
    }

    @Transactional(readOnly = true)
    public List<MemberSanctionResponse> getMemberSanctions(Long memberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }

        return sanctionRepository
                .findByMemberIdOrderByStartedAtDesc(memberId)
                .stream()
                .map(MemberSanctionResponse::from)
                .toList();
    }

    // 관리자 회원 목록 및 검색
    @Transactional(readOnly = true)
    public Page<AdminMemberResponse> getMembers(
            String keyword,
            Pageable pageable
    ) {
        Page<Member> members;

        if (!StringUtils.hasText(keyword)) {
            members = memberRepository.findAll(pageable);
        } else {
            String searchKeyword = keyword.trim();

            members = memberRepository
                    .findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                            searchKeyword,
                            searchKeyword,
                            pageable
                    );
        }

        return members.map(AdminMemberResponse::from);
    }

    // 관리자 회원 상세 조회
    @Transactional(readOnly = true)
    public AdminMemberResponse getMember(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        return AdminMemberResponse.from(member);
    }

}