package com.needitbe.domain.member.dto;

import com.needitbe.domain.member.entity.Member;

public record MemberResponse(
        Long memberId,
        String username,
        String nickname,
        String region,
        String role
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getNickname(),
                member.getRegion(),
                member.getRole().name()
        );
    }
}