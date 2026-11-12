package com.needitbe.domain.block.dto;

import com.needitbe.domain.block.entity.Block;

import java.time.LocalDateTime;

public record BlockResponse(

        Long blockId,
        Long blockedMemberId,
        String blockedNickname,
        LocalDateTime createdAt

) {

    public static BlockResponse from(Block block) {

        return new BlockResponse(
                block.getId(),
                block.getBlocked().getId(),
                block.getBlocked().getNickname(),
                block.getCreatedAt()
        );
    }
}