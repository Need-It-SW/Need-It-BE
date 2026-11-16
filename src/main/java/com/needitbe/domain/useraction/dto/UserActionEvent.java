package com.needitbe.domain.useraction.dto;

public record UserActionEvent(
        Long userId,
        String action,
        String keyword
) {
}