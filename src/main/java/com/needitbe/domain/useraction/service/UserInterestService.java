package com.needitbe.domain.useraction.service;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.entity.UserActionFilterResult;
import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInterestService {

    private final UserActionLogRepository userActionLogRepository;

    // ==========================================
    // 1. 회원별 카테고리 관심도 계산
    // ==========================================
    public Map<RequestCategory, Long> getCategoryInterestScores(
            Long memberId
    ) {

        List<Object[]> results =
                userActionLogRepository.findCategoryInterestScores(
                        memberId,
                        UserActionFilterResult.INCLUDED
                );

        Map<RequestCategory, Long> scores = new LinkedHashMap<>();

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
    // 2. 가장 관심도가 높은 카테고리 조회
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
    // 3. 회원별 최근 검색 키워드 조회
    // ==========================================
    public Set<String> getRecentSearchKeywords(
            Long memberId
    ) {

        List<UserActionLog> logs =
                userActionLogRepository
                        .findTop20ByMemberIdAndActionAndKeywordIsNotNullAndFilterResultOrderByCreatedAtDescIdDesc(
                                memberId,
                                "SEARCH",
                                UserActionFilterResult.INCLUDED
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

    // ==========================================
    // 4. 회원별 최근 행동 로그 조회
    // ==========================================
    public List<UserActionLog> getRecentActions(
            Long memberId
    ) {

        return userActionLogRepository
                .findTop100ByMemberIdAndFilterResultOrderByCreatedAtDescIdDesc(
                        memberId,
                        UserActionFilterResult.INCLUDED
                );
    }
}