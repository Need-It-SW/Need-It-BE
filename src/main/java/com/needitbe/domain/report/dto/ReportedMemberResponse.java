package com.needitbe.domain.report.dto;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.entity.MemberStatus;

public record ReportedMemberResponse(
        Long memberId,
        String username,
        String nickname,
        long reportCount,
        long pendingCount,
        long inReviewCount,
        long resolvedCount,
        long rejectedCount,
        MemberStatus status
) {

    public static ReportedMemberResponse from(
            Member member,
            long reportCount,
            long pendingCount,
            long inReviewCount,
            long resolvedCount,
            long rejectedCount
    ) {
        return new ReportedMemberResponse(
                member.getId(),
                member.getUsername(),
                member.getNickname(),
                reportCount,
                pendingCount,
                inReviewCount,
                resolvedCount,
                rejectedCount,
                member.getStatus()
        );
    }
}