package com.needitbe.domain.admin.dto;

import com.needitbe.domain.admin.entity.MemberSanction;

import java.time.LocalDateTime;

public record AdminSanctionHistoryResponse(

        Long sanctionId,
        Long memberId,
        String username,
        String nickname,
        Long adminId,
        String reason,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime releasedAt,
        boolean active

) {

    public static AdminSanctionHistoryResponse from(
            MemberSanction sanction
    ) {

        return new AdminSanctionHistoryResponse(
                sanction.getId(),
                sanction.getMember().getId(),
                sanction.getMember().getUsername(),
                sanction.getMember().getNickname(),
                sanction.getAdmin().getId(),
                sanction.getReason(),
                sanction.getStartedAt(),
                sanction.getExpiresAt(),
                sanction.getReleasedAt(),
                sanction.isActive()
        );
    }
}