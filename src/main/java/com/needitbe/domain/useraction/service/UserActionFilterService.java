package com.needitbe.domain.useraction.service;

import com.needitbe.domain.admin.entity.MemberSanction;
import com.needitbe.domain.admin.repository.MemberSanctionRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.entity.MemberStatus;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.useraction.dto.UserActionFilterDecision;
import com.needitbe.domain.useraction.entity.UserActionFilterReason;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserActionFilterService {

    private final MemberRepository memberRepository;
    private final MemberSanctionRepository sanctionRepository;

    @Transactional(readOnly = true)
    public UserActionFilterDecision evaluate(Long memberId) {

        // 1. 비회원 행동은 기존 정책대로 분석 반영
        if (memberId == null) {
            return UserActionFilterDecision.included();
        }

        // 2. 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElse(null);

        if (member == null) {
            return UserActionFilterDecision.excluded(
                    UserActionFilterReason.MEMBER_NOT_FOUND,
                    null
            );
        }

        // 3. 정상 회원은 분석 반영
        if (member.getStatus() == MemberStatus.ACTIVE) {
            return UserActionFilterDecision.included();
        }

        // 4. 정지된 회원의 제재 이력 확인
        MemberSanction sanction = sanctionRepository
                .findFirstByMemberIdAndReleasedAtIsNullOrderByStartedAtDesc(
                        memberId
                )
                .orElse(null);

        // 5. 유효한 제재가 있으면 분석 제외
        if (sanction != null && sanction.isActive()) {
            return UserActionFilterDecision.excluded(
                    UserActionFilterReason.MEMBER_SUSPENDED,
                    sanction.getId()
            );
        }

        // 6. 제재 기간이 만료됐거나 유효한 제재가 없으면 분석 반영
        return UserActionFilterDecision.included();
    }
}