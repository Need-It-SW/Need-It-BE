package com.needitbe.domain.search.service;

import com.needitbe.domain.search.entity.SearchLog;
import com.needitbe.domain.search.repository.SearchLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.needitbe.domain.useraction.kafka.UserActionProducer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchLogRepository searchLogRepository;
    private final UserActionProducer userActionProducer;
    private final SimpMessagingTemplate messagingTemplate;

    // =====================================================
    // 1. 검색 기록 저장
    // =====================================================
    @Transactional
    public void saveSearch(String keyword, Long memberId) {

        // 1. 검색어 검증
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        // 2. 검색어 정규화
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

        // 4. 기존 MySQL 검색 기록 저장
        SearchLog searchLog = new SearchLog(normalizedKeyword);
        searchLogRepository.save(searchLog);

        // 5. Kafka 이벤트 생성
        String userId = memberId == null
                ? "anonymous"
                : memberId.toString();
        String eventId = UUID.randomUUID().toString();


        String message = """
        {
          "eventId": "%s",
          "userId": %s,
          "action": "SEARCH",
          "keyword": %s
        }
        """.formatted(
                eventId,
                memberId == null ? "null" : memberId.toString(),
                "\"" + normalizedKeyword
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"") + "\""
        );

        // 6. DB 커밋 후 Kafka 메시지 전송
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            userActionProducer.send(userId, message);
                        } catch (Exception ex) {
                            org.slf4j.LoggerFactory.getLogger(SearchService.class)
                                    .warn("검색 행동 Kafka 발행 실패", ex);
                        }
                        try {
                            messagingTemplate.convertAndSend("/topic/searches/popular", "UPDATED");
                        } catch (Exception ex) {
                            org.slf4j.LoggerFactory.getLogger(SearchService.class)
                                    .warn("인기 검색어 실시간 알림 전송 실패", ex);
                        }
                    }
                }
        );
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