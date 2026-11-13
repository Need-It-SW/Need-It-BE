package com.needitbe.domain.admin.dto;

import com.needitbe.domain.admin.entity.MemberSanction;

import java.time.LocalDateTime;

public record MemberSanctionResponse(
        Long id,
        Long memberId,
        Long adminId,
        String reason,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime releasedAt,
        String status
) {

    public static MemberSanctionResponse from(MemberSanction sanction) {

        String status;

        if (sanction.getReleasedAt() != null) {
            status = "RELEASED";
        } else if (sanction.getExpiresAt() != null
                && !LocalDateTime.now().isBefore(sanction.getExpiresAt())) {
            status = "EXPIRED";
        } else {
            status = "ACTIVE";
        }

        return new MemberSanctionResponse(
                sanction.getId(),
                sanction.getMember().getId(),
                sanction.getAdmin().getId(),
                sanction.getReason(),
                sanction.getStartedAt(),
                sanction.getExpiresAt(),
                sanction.getReleasedAt(),
                status
        );
    }
}