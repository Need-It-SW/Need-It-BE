package com.needitbe.domain.admin.dto;

import java.util.List;

public record AdminUserActionStatisticsResponse(
        long totalCount,
        List<ActionCount> actionCounts
) {
    public record ActionCount(
            String action,
            long count
    ) {
    }
}