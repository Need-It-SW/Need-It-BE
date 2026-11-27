package com.needitbe.domain.useraction.service;

import com.needitbe.domain.useraction.entity.UserActionFilterResult;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerBehaviorService {

    private static final int VIEW_WEIGHT = 2;
    private static final int FAVORITE_WEIGHT = 3;
    private static final int MIN_INTEREST_COUNT = 10;
    private static final int TOP_LIMIT = 5;

    private static final List<String> ACTION_TYPES = List.of(
            "SEARCH",
            "VIEW",
            "CATEGORY_CLICK",
            "FAVORITE",
            "REQUEST_CREATE",
            "OFFER"
    );

    private final UserActionLogRepository repository;

    @Transactional(readOnly = true)
    public Response analyze(String period, LocalDate from, LocalDate to) {

        AnalysisPeriod analysisPeriod = resolvePeriod(period, from, to);

        UserActionFilterResult filterResult =
                UserActionFilterResult.INCLUDED;

        List<Object[]> currentRows = repository.behaviorGroups(
                filterResult,
                analysisPeriod.start(),
                analysisPeriod.end()
        );

        List<Object[]> previousRows = repository.behaviorGroups(
                filterResult,
                analysisPeriod.previousStart(),
                analysisPeriod.previousEnd()
        );

        BehaviorStatistics current = aggregateBehavior(currentRows);

        Map<String, Long> previousScores =
                calculateInterestScores(previousRows);

        long activeMembers = repository.behaviorMembers(
                filterResult,
                analysisPeriod.start(),
                analysisPeriod.end()
        );

        List<Category> topCategories =
                getTopCategories(current.categoryCounts());

        List<Rising> risingCategories =
                getRisingCategories(current, previousScores);

        return new Response(
                activeMembers,
                current.totalActions(),
                current.actionCounts(),
                topCategories,
                risingCategories,
                analysisPeriod.start(),
                analysisPeriod.end(),
                analysisPeriod.previousStart(),
                analysisPeriod.previousEnd()
        );
    }

    // 분석 기간 및 이전 비교 기간 설정
    private AnalysisPeriod resolvePeriod(
            String period,
            LocalDate from,
            LocalDate to
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        if ("TODAY".equals(period)) {
            LocalDateTime start = today.atStartOfDay();

            return new AnalysisPeriod(
                    start,
                    now,
                    start.minusDays(1),
                    now.minusDays(1)
            );
        }

        switch (period) {
            case "7D" -> {
                from = today.minusDays(6);
                to = today;
            }

            case "30D" -> {
                from = today.minusDays(29);
                to = today;
            }

            case "CUSTOM" -> {
                // 사용자가 선택한 기간 사용
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid period"
            );
        }

        validateDateRange(from, to, today);

        LocalDateTime start = from.atStartOfDay();

        LocalDateTime end = to.equals(today)
                ? now
                : to.plusDays(1).atStartOfDay();

        long days = ChronoUnit.DAYS.between(from, to) + 1;

        return new AnalysisPeriod(
                start,
                end,
                start.minusDays(days),
                end.minusDays(days)
        );
    }

    // 사용자 지정 기간 검증
    private void validateDateRange(
            LocalDate from,
            LocalDate to,
            LocalDate today
    ) {
        if (from == null
                || to == null
                || from.isAfter(to)
                || to.isAfter(today)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date range"
            );
        }
    }

    // 현재 기간의 사용자 행동 데이터 집계
    private BehaviorStatistics aggregateBehavior(List<Object[]> rows) {

        Map<String, Long> actionCounts = initializeActionCounts();
        Map<String, Long> categoryCounts = new HashMap<>();
        Map<String, Long> interestScores = new HashMap<>();
        Map<String, Long> interestCounts = new HashMap<>();

        long totalActions = 0;

        for (Object[] row : rows) {

            String action = row[0].toString();
            long count = ((Number) row[2]).longValue();

            totalActions += count;

            actionCounts.merge(action, count, Long::sum);

            if (row[1] == null) {
                continue;
            }

            String category = row[1].toString();

            categoryCounts.merge(category, count, Long::sum);

            int weight = getActionWeight(action);

            if (weight > 0) {
                interestScores.merge(
                        category,
                        count * weight,
                        Long::sum
                );

                interestCounts.merge(
                        category,
                        count,
                        Long::sum
                );
            }
        }

        return new BehaviorStatistics(
                totalActions,
                actionCounts,
                categoryCounts,
                interestScores,
                interestCounts
        );
    }

    // 행동 유형별 초기 집계 데이터 생성
    private Map<String, Long> initializeActionCounts() {

        Map<String, Long> actionCounts = new LinkedHashMap<>();

        for (String action : ACTION_TYPES) {
            actionCounts.put(action, 0L);
        }

        return actionCounts;
    }

    // 이전 기간의 카테고리별 관심도 점수 계산
    private Map<String, Long> calculateInterestScores(
            List<Object[]> rows
    ) {
        Map<String, Long> interestScores = new HashMap<>();

        for (Object[] row : rows) {

            if (row[1] == null) {
                continue;
            }

            String action = row[0].toString();
            String category = row[1].toString();
            long count = ((Number) row[2]).longValue();

            int weight = getActionWeight(action);

            if (weight > 0) {
                interestScores.merge(
                        category,
                        count * weight,
                        Long::sum
                );
            }
        }

        return interestScores;
    }

    // 인기 카테고리 TOP 5 조회
    private List<Category> getTopCategories(
            Map<String, Long> categoryCounts
    ) {
        return categoryCounts.entrySet()
                .stream()
                .map(entry -> new Category(
                        entry.getKey(),
                        entry.getValue()
                ))
                .sorted(
                        Comparator.comparingLong(Category::count)
                                .reversed()
                                .thenComparing(Category::category)
                )
                .limit(TOP_LIMIT)
                .toList();
    }

    // 급상승 관심 카테고리 TOP 5 조회
    private List<Rising> getRisingCategories(
            BehaviorStatistics current,
            Map<String, Long> previousScores
    ) {
        return current.interestScores()
                .entrySet()
                .stream()
                .filter(entry -> isRisingCategory(
                        entry.getKey(),
                        entry.getValue(),
                        current.interestCounts(),
                        previousScores
                ))
                .map(entry -> createRisingCategory(
                        entry.getKey(),
                        entry.getValue(),
                        current.interestCounts(),
                        previousScores
                ))
                .sorted(
                        Comparator.comparing(
                                        Rising::growthRate,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                                .thenComparing(
                                        Comparator.comparingLong(
                                                Rising::currentScore
                                        ).reversed()
                                )
                                .thenComparing(Rising::category)
                )
                .limit(TOP_LIMIT)
                .toList();
    }

    // 급상승 카테고리 선정 조건 확인
    private boolean isRisingCategory(
            String category,
            long currentScore,
            Map<String, Long> interestCounts,
            Map<String, Long> previousScores
    ) {
        long currentCount = interestCounts.getOrDefault(category, 0L);
        long previousScore = previousScores.getOrDefault(category, 0L);

        return currentCount >= MIN_INTEREST_COUNT
                && currentScore > previousScore;
    }

    // 급상승 카테고리 응답 데이터 생성
    private Rising createRisingCategory(
            String category,
            long currentScore,
            Map<String, Long> interestCounts,
            Map<String, Long> previousScores
    ) {
        long previousScore = previousScores.getOrDefault(category, 0L);
        long currentCount = interestCounts.getOrDefault(category, 0L);

        Double growthRate = calculateGrowthRate(
                previousScore,
                currentScore
        );

        return new Rising(
                category,
                previousScore,
                currentScore,
                currentCount,
                growthRate
        );
    }

    // 이전 기간 대비 관심도 증가율 계산
    private Double calculateGrowthRate(
            long previousScore,
            long currentScore
    ) {
        if (previousScore == 0) {
            return null;
        }

        return 100.0 * (currentScore - previousScore) / previousScore;
    }

    // 개인화 광고와 동일한 관심도 가중치 적용
    private int getActionWeight(String action) {

        return switch (action) {
            case "VIEW" -> VIEW_WEIGHT;
            case "FAVORITE" -> FAVORITE_WEIGHT;
            default -> 0;
        };
    }

    // 내부 분석 기간
    private record AnalysisPeriod(
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime previousStart,
            LocalDateTime previousEnd
    ) {
    }

    // 내부 행동 집계 데이터
    private record BehaviorStatistics(
            long totalActions,
            Map<String, Long> actionCounts,
            Map<String, Long> categoryCounts,
            Map<String, Long> interestScores,
            Map<String, Long> interestCounts
    ) {
    }

    public record Category(
            String category,
            long count
    ) {
    }

    public record Rising(
            String category,
            long previousScore,
            long currentScore,
            long currentCount,
            Double growthRate
    ) {
    }

    public record Response(
            long activeMembers,
            long totalActions,
            Map<String, Long> actions,
            List<Category> categories,
            List<Rising> rising,
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime previousStart,
            LocalDateTime previousEnd
    ) {
    }
}