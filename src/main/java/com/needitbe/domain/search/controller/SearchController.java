package com.needitbe.domain.search.controller;

import com.needitbe.domain.search.service.SearchService;
import com.needitbe.domain.search.service.SearchService.PopularKeywordResponse;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/searches")
public class SearchController {

    private final SearchService searchService;

    // =====================================================
    // 1. 검색 기록 저장
    // =====================================================
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveSearch(
            @RequestBody SearchRequest request
    ) {

        searchService.saveSearch(request.keyword());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "검색 기록 저장에 성공했습니다.",
                        null
                )
        );
    }

    // =====================================================
    // 2. 인기 검색어 TOP 10 조회
    // =====================================================
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularKeywordResponse>>>
    getPopularKeywords() {

        List<PopularKeywordResponse> response =
                searchService.getPopularKeywords();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "인기 검색어 조회에 성공했습니다.",
                        response
                )
        );
    }

    // 검색 기록 저장 요청 DTO
    public record SearchRequest(
            String keyword
    ) {
    }
}