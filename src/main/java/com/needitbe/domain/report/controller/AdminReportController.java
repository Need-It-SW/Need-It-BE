package com.needitbe.domain.report.controller;

import com.needitbe.domain.report.dto.ReportResponse;
import com.needitbe.domain.report.dto.ReportStatusUpdateRequest;
import com.needitbe.domain.report.entity.ReportStatus;
import com.needitbe.domain.report.service.ReportService;
import com.needitbe.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    // 1. 전체 및 상태별 신고 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(

            @RequestParam(required = false)
            ReportStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "관리자 신고 목록 조회에 성공했습니다.",
                        reportService.getAllReports(
                                status,
                                page,
                                size
                        )
                )
        );
    }

    // 2. 신고 상세 조회
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable Long reportId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "신고 상세 조회에 성공했습니다.",
                        reportService.getReport(reportId)
                )
        );
    }

    // 3. 신고 상태 변경
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<ApiResponse<ReportResponse>> updateStatus(

            @PathVariable Long reportId,

            @Valid @RequestBody
            ReportStatusUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "신고 상태 변경에 성공했습니다.",
                        reportService.updateReportStatus(
                                reportId,
                                request
                        )
                )
        );
    }
}