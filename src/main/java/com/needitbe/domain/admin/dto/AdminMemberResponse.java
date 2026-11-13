package com.needitbe.domain.admin.dto;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.entity.MemberRole;
import com.needitbe.domain.member.entity.MemberStatus;

import java.time.LocalDateTime;

public record AdminMemberResponse(
        Long memberId,
        String username,
        String nickname,
        String region,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt
) {
    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getUsername(),
                member.getNickname(),
                member.getRegion(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt()
        );
    }
}