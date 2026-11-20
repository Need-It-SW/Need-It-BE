package com.needitbe.domain.admin.controller;

import com.needitbe.domain.admin.dto.DashboardCampaignMatchResponse;
import com.needitbe.domain.admin.dto.DashboardCategoryResponse;
import com.needitbe.domain.admin.dto.DashboardSummaryResponse;
import com.needitbe.domain.admin.service.DashboardService;
import com.needitbe.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<DashboardCategoryResponse>>> getCategories() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "카테고리별 행동 로그 통계 조회에 성공했습니다.",
                        dashboardService.getCategoryStatistics()
                )
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "관리자 대시보드 요약 조회에 성공했습니다.",
                        dashboardService.getSummary()
                )
        );
    }

    @GetMapping("/campaign-matches")
    public ResponseEntity<ApiResponse<List<DashboardCampaignMatchResponse>>> getCampaignMatches() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "캠페인별 조건 충족 로그 조회에 성공했습니다.",
                        dashboardService.getCampaignMatches()
                )
        );
    }
}