package com.needitbe.domain.useraction.controller;

import com.needitbe.domain.admin.dto.AdminUserActionStatisticsResponse;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.dto.response.AdminUserActionResponse;
import com.needitbe.domain.useraction.entity.UserActionFilterResult;
import com.needitbe.domain.useraction.entity.UserActionFilterReason;
import com.needitbe.domain.useraction.service.AdminUserActionService;
import com.needitbe.domain.useraction.dto.AdminUserActionFilterStatisticsResponse;


import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/user-actions")
@RequiredArgsConstructor
public class AdminUserActionController {

    private final AdminUserActionService adminUserActionService;

    @GetMapping
    public Page<AdminUserActionResponse> getLogs(

            @RequestParam(required = false)
            String action,

            @RequestParam(required = false)
            RequestCategory category,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            Long memberId,

            @RequestParam(required = false)
            UserActionFilterResult filterResult,

            @RequestParam(required = false)
            UserActionFilterReason filterReason,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return adminUserActionService.getLogs(
                action,
                category,
                keyword,
                search,
                startDate,
                endDate,
                memberId,
                filterResult,
                filterReason,
                pageable
        );
    }

    @GetMapping("/statistics")
    public AdminUserActionStatisticsResponse getStatistics() {
        return adminUserActionService.getStatistics();
    }

    @GetMapping("/filter-statistics")
    public AdminUserActionFilterStatisticsResponse getFilterStatistics() {
        return adminUserActionService.getFilterStatistics();
    }
}