package com.needitbe.domain.search.service;

import com.needitbe.domain.search.entity.SearchLog;
import com.needitbe.domain.search.repository.SearchLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchLogRepository searchLogRepository;

    // =====================================================
    // 1. 검색 기록 저장
    // =====================================================
    @Transactional
    public void saveSearch(String keyword) {

        // 1. 검색어가 없으면 저장하지 않음
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        // 2. 앞뒤 공백 제거 + 연속 공백 정리 + 소문자 변환
        String normalizedKeyword = keyword
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);

        // 3. 검색어 길이 제한
        if (normalizedKeyword.length() > 100) {
            throw new IllegalArgumentException(
                    "검색어는 100자 이하로 입력해야 합니다."
            );
        }

        // 4. 검색 기록 생성
        SearchLog searchLog = new SearchLog(normalizedKeyword);

        // 5. MySQL 저장
        searchLogRepository.save(searchLog);
    }

    // =====================================================
    // 2. 최근 7일 인기 검색어 TOP 10 조회
    // =====================================================
    @Transactional(readOnly = true)
    public List<PopularKeywordResponse> getPopularKeywords() {

        // 1. 현재 시간 기준 7일 전 계산
        LocalDateTime startDate =
                LocalDateTime.now().minusDays(7);

        // 2. 검색어별 검색 횟수 조회
        List<Object[]> results =
                searchLogRepository.findPopularKeywords(startDate);

        // 3. TOP 10 결과 생성
        List<PopularKeywordResponse> popularKeywords =
                new ArrayList<>();

        int rank = 1;

        for (Object[] result : results) {

            // 상위 10개까지만 반환
            if (rank > 10) {
                break;
            }

            String keyword = (String) result[0];
            Long searchCount = (Long) result[1];

            popularKeywords.add(
                    new PopularKeywordResponse(
                            rank,
                            keyword,
                            searchCount
                    )
            );

            rank++;
        }

        return popularKeywords;
    }

    // =====================================================
    // 3. 인기 검색어 응답 DTO
    // =====================================================
    public record PopularKeywordResponse(
            int rank,
            String keyword,
            Long searchCount
    ) {
    }
}