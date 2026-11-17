package com.needitbe.domain.useraction.service;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.needitbe.domain.useraction.entity.UserActionLog;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInterestService {

    private final UserActionLogRepository userActionLogRepository;

    // ==========================================
    // 회원별 카테고리 관심도 계산
    // ==========================================
    public Map<RequestCategory, Long> getCategoryInterestScores(
            Long memberId
    ) {

        List<Object[]> results =
                userActionLogRepository.findCategoryInterestScores(memberId);

        Map<RequestCategory, Long> scores =
                new LinkedHashMap<>();

        for (Object[] row : results) {

            RequestCategory category =
                    (RequestCategory) row[0];

            Long score =
                    ((Number) row[1]).longValue();

            scores.put(category, score);
        }

        return scores;
    }

    // ==========================================
    // 가장 관심도가 높은 카테고리 조회
    // ==========================================
    public RequestCategory getTopInterestCategory(
            Long memberId
    ) {

        Map<RequestCategory, Long> scores =
                getCategoryInterestScores(memberId);

        if (scores.isEmpty()) {
            return null;
        }

        return scores.keySet()
                .iterator()
                .next();
    }

    // ==========================================
// 회원별 최근 검색 키워드 조회
// ==========================================
    public Set<String> getRecentSearchKeywords(Long memberId) {

        List<UserActionLog> logs =
                userActionLogRepository
                        .findTop20ByMemberIdAndActionAndKeywordIsNotNullOrderByCreatedAtDescIdDesc(
                                memberId,
                                "SEARCH"
                        );

        Set<String> keywords = new LinkedHashSet<>();

        for (UserActionLog log : logs) {

            String keyword = log.getKeyword();

            if (keyword == null || keyword.isBlank()) {
                continue;
            }

            keywords.add(
                    keyword.trim().toLowerCase(Locale.ROOT)
            );
        }

        return keywords;
    }
}