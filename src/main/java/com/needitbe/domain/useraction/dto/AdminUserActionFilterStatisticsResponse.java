package com.needitbe.domain.useraction.dto;

import java.util.Map;

public record AdminUserActionFilterStatisticsResponse(
        long totalCount,
        long includedCount,
        long excludedCount,
        long unclassifiedCount,
        double exclusionRate,
        Map<String, Long> reasonCounts
) {
}