package com.needitbe.domain.report.controller;

import com.needitbe.domain.report.dto.ReportCreateRequest;
import com.needitbe.domain.report.dto.ReportResponse;
import com.needitbe.domain.report.service.ReportService;
import com.needitbe.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 신고 접수
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            Authentication authentication,
            @Valid @RequestBody ReportCreateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "신고가 접수되었습니다.",
                        reportService.createReport(memberId, request)
                )
        );
    }

    // 내가 신고한 내역 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReports(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내 신고 내역 조회에 성공했습니다.",
                        reportService.getMyReports(memberId)
                )
        );
    }
}