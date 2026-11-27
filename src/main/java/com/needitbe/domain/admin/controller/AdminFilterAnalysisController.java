package com.needitbe.domain.admin.controller;

import com.needitbe.domain.admin.dto.FilterAnalysisSummaryResponse;
import com.needitbe.domain.admin.dto.FilterReasonResponse;
import com.needitbe.domain.admin.service.AdminFilterAnalysisService;

import com.needitbe.domain.admin.dto.FilterExclusionResponse;
import com.needitbe.domain.ads.entity.AdExclusionReasonType;

import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.needitbe.domain.admin.dto.FilterAdEventResponse;
import com.needitbe.domain.ads.entity.AdEventType;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/filter-analysis")
public class AdminFilterAnalysisController {

    private final AdminFilterAnalysisService filterAnalysisService;

    // ==========================================
    // 1. 고객 필터링 요약 통계
    // ==========================================

    @GetMapping("/campaign-breakdown")
    public List<java.util.Map<String, Object>> getCampaignBreakdown(@RequestParam(defaultValue = "7D") String period) {
        return filterAnalysisService.getCampaignBreakdown(period);
    }

    @GetMapping("/evaluations")
    public Page<java.util.Map<String, Object>> getEvaluations(
            @RequestParam(defaultValue = "7D") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) com.needitbe.domain.ads.entity.AdFilterResult result,
            @RequestParam(defaultValue = "10") int size) {
        return filterAnalysisService.getEvaluations(period, campaignId, result, page, size);
    }

    @GetMapping("/summary")
    public FilterAnalysisSummaryResponse getSummary(

            @RequestParam(required = false) Long campaignId,

            @RequestParam(defaultValue = "7D")
            String period

    ) {

        return filterAnalysisService.getSummary(
                campaignId,
                period
        );
    }

    // ==========================================
    // 2. 제외 사유별 통계
    // ==========================================

    @GetMapping("/reasons")
    public List<FilterReasonResponse> getReasons(

            @RequestParam(required = false) Long campaignId,

            @RequestParam(defaultValue = "7D")
            String period

    ) {

        return filterAnalysisService.getReasons(
                campaignId,
                period
        );
    }

    @GetMapping("/exclusions")
    public Page<FilterExclusionResponse> getExclusions(

            @RequestParam(required = false) Long campaignId,

            @RequestParam(defaultValue = "7D")
            String period,

            @RequestParam(required = false)
            AdExclusionReasonType reason,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size

    ) {

        return filterAnalysisService.getExclusions(
                campaignId,
                period,
                reason,
                page,
                size
        );
    }

    @GetMapping("/ad-events")
    public Page<FilterAdEventResponse> getAdEvents(

            @RequestParam(required = false) Long campaignId,

            @RequestParam(defaultValue = "7D")
            String period,

            @RequestParam(required = false)
            AdEventType eventType,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size

    ) {

        return filterAnalysisService.getAdEvents(
                campaignId,
                period,
                eventType,
                page,
                size
        );
    }
}