package com.needitbe.domain.admin.dto;

public record FilterAnalysisSummaryResponse(

        Long campaignId,

        // 자동 매칭 평가
        long totalEvaluations,

        // 광고 후보 선정
        long selectedCount,

        // 광고 후보 제외
        long excludedCount,

        // 후보 선정률 (%)
        double selectionRate

) {
}