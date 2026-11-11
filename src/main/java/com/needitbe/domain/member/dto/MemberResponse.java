package com.needitbe.domain.member.dto;

import com.needitbe.domain.member.entity.Member;

public record MemberResponse(
        Long memberId,
        String username,
        String nickname,
        String region,
        String role,
        String profileImageUrl
) {

    // 기존 호출 코드와 호환
    public static MemberResponse from(Member member) {
        return from(member, null);
    }

    // 프로필 사진 URL 포함
    public static MemberResponse from(
            Member member,
            String profileImageUrl
    ) {
        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getNickname(),
                member.getRegion(),
                member.getRole().name(),
                profileImageUrl
        );
    }
}