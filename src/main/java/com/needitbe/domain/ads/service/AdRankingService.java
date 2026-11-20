package com.needitbe.domain.ads.service;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.entity.UserActionLog;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdRankingService {

    private static final double CATEGORY_WEIGHT = 0.4;
    private static final double KEYWORD_WEIGHT = 0.4;
    private static final double RECENCY_WEIGHT = 0.2;

    public double calculateScore(
            AdCampaign campaign,
            List<UserActionLog> userActions,
            Map<RequestCategory, Long> interestScores
    ) {

        double categoryScore =
                calculateCategoryScore(campaign, interestScores);

        double keywordScore =
                calculateKeywordScore(campaign, userActions);

        double recencyScore =
                calculateRecencyScore(campaign, userActions);

        return categoryScore * CATEGORY_WEIGHT
                + keywordScore * KEYWORD_WEIGHT
                + recencyScore * RECENCY_WEIGHT;
    }

    // 1. 관심 카테고리 점수
    private double calculateCategoryScore(
            AdCampaign campaign,
            Map<RequestCategory, Long> interestScores
    ) {

        String targetCategory = campaign.getTargetCategory();

        if (targetCategory == null || targetCategory.isBlank()) {
            return 0;
        }

        long maxScore = interestScores.values()
                .stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        if (maxScore == 0) {
            return 0;
        }

        long score = interestScores.entrySet()
                .stream()
                .filter(entry ->
                        entry.getKey().name()
                                .equalsIgnoreCase(targetCategory.trim())
                )
                .mapToLong(Map.Entry::getValue)
                .findFirst()
                .orElse(0);

        return (double) score / maxScore * 100;
    }

    // 2. 검색 키워드 점수
    private double calculateKeywordScore(
            AdCampaign campaign,
            List<UserActionLog> userActions
    ) {

        String targetKeywords = campaign.getTargetKeywords();

        if (targetKeywords == null || targetKeywords.isBlank()) {
            return 0;
        }

        List<String> keywords = Arrays.stream(
                        targetKeywords.split(",")
                )
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .toList();

        long matchCount = userActions.stream()
                .filter(action ->
                        "SEARCH".equals(action.getAction())
                )
                .filter(action -> action.getKeyword() != null)
                .filter(action ->
                        keywords.contains(
                                action.getKeyword()
                                        .trim()
                                        .toLowerCase(Locale.ROOT)
                        )
                )
                .count();

        // 검색 횟수 5회 이상이면 최고 점수
        return Math.min(100, matchCount * 20);
    }

    // 3. 최근 행동 점수
    private double calculateRecencyScore(
            AdCampaign campaign,
            List<UserActionLog> userActions
    ) {

        String targetCategory = campaign.getTargetCategory();

        if (targetCategory == null || targetCategory.isBlank()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();

        return userActions.stream()
                .filter(action -> action.getCategory() != null)
                .filter(action ->
                        action.getCategory().name()
                                .equalsIgnoreCase(targetCategory.trim())
                )
                .mapToDouble(action -> {

                    long hours = Duration.between(
                            action.getCreatedAt(),
                            now
                    ).toHours();

                    if (hours < 0) {
                        return 0;
                    }

                    if (hours < 1) {
                        return 100;
                    }

                    if (hours < 24) {
                        return 80;
                    }

                    if (hours < 72) {
                        return 50;
                    }

                    if (hours < 168) {
                        return 20;
                    }

                    return 0;
                })
                .max()
                .orElse(0);
    }
}