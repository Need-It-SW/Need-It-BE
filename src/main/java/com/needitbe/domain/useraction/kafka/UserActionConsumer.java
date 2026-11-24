package com.needitbe.domain.useraction.kafka;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.dto.UserActionFilterDecision;
import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;
import com.needitbe.domain.useraction.service.UserActionFilterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.needitbe.domain.useraction.websocket.UserActionWebSocketPublisher;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserActionWebSocketPublisher userActionWebSocketPublisher;
    private final ObjectMapper objectMapper;
    private final UserActionLogRepository userActionLogRepository;
    private final UserActionFilterService userActionFilterService;

    @KafkaListener(
            topics = "needit-user-actions",
            groupId = "needit-user-actions-group"
    )
    @Transactional
    public void consume(String message) {

        try {
            JsonNode json = objectMapper.readTree(message);

            if (json == null || !json.isObject()) {
                log.warn("유효하지 않은 Kafka JSON 객체");
                return;
            }

            // 1. eventId 검증
            JsonNode eventIdNode = json.get("eventId");

            if (eventIdNode == null || !eventIdNode.isTextual()) {
                log.warn("eventId가 없는 이벤트");
                return;
            }

            String eventId = eventIdNode.asText();

            try {
                UUID.fromString(eventId);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 eventId 형식: {}", eventId);
                return;
            }

            // 2. 중복 이벤트 확인
            if (userActionLogRepository.existsByEventId(eventId)) {
                log.info(
                        "이미 처리한 Kafka 이벤트 - eventId: {}",
                        eventId
                );
                return;
            }

            // 3. 회원 ID 추출
            Long memberId = null;

            JsonNode userIdNode = json.get("userId");

            if (userIdNode != null && !userIdNode.isNull()) {

                if (!userIdNode.isIntegralNumber()
                        || !userIdNode.canConvertToLong()) {

                    log.warn("잘못된 사용자 ID");
                    return;
                }

                memberId = userIdNode.longValue();

                if (memberId <= 0) {
                    log.warn("유효하지 않은 사용자 ID");
                    return;
                }
            }

            // 4. 행동 유형 확인
            JsonNode actionNode = json.get("action");

            if (actionNode == null || !actionNode.isTextual()) {
                log.warn("행동 유형이 없는 이벤트");
                return;
            }

            String action = actionNode.asText();

            // 5. SEARCH 처리
            if ("SEARCH".equals(action)) {

                JsonNode keywordNode = json.get("keyword");

                if (keywordNode == null || !keywordNode.isTextual()) {
                    log.warn("검색어가 없는 이벤트");
                    return;
                }

                String keyword = keywordNode.asText();

                if (keyword.isBlank() || keyword.length() > 100) {
                    log.warn("유효하지 않은 검색어");
                    return;
                }

                saveActionLog(
                        new UserActionLog(
                                eventId,
                                memberId,
                                action,
                                keyword
                        )
                );

                log.info(
                        "SEARCH 행동 저장 - memberId: {}, keyword: {}",
                        memberId,
                        keyword
                );

                return;
            }

            // 6. CATEGORY_CLICK 처리
            if ("CATEGORY_CLICK".equals(action)) {

                if (memberId == null) {
                    log.warn("회원 ID가 없는 카테고리 클릭 이벤트");
                    return;
                }

                JsonNode categoryNode = json.get("category");

                if (categoryNode == null || !categoryNode.isTextual()) {
                    log.warn("카테고리가 없는 클릭 이벤트");
                    return;
                }

                RequestCategory category;

                try {
                    category = RequestCategory.valueOf(
                            categoryNode.asText()
                    );
                } catch (IllegalArgumentException e) {
                    log.warn(
                            "지원하지 않는 카테고리: {}",
                            categoryNode.asText()
                    );
                    return;
                }

                saveActionLog(
                        new UserActionLog(
                                eventId,
                                memberId,
                                action,
                                null,
                                null,
                                category
                        )
                );

                log.info(
                        "CATEGORY_CLICK 저장 - memberId: {}, category: {}",
                        memberId,
                        category
                );

                return;
            }

            // 7. REQUEST_CREATE 처리
            if ("REQUEST_CREATE".equals(action)) {

                if (memberId == null) {
                    log.warn("회원 ID가 없는 요청 등록 이벤트");
                    return;
                }

                // 게시글 ID 확인
                JsonNode requestIdNode = json.get("requestId");

                if (requestIdNode == null
                        || !requestIdNode.isIntegralNumber()
                        || !requestIdNode.canConvertToLong()) {

                    log.warn("유효하지 않은 요청 게시글 ID");
                    return;
                }

                Long requestId = requestIdNode.longValue();

                if (requestId <= 0) {
                    log.warn("게시글 ID는 1 이상이어야 합니다.");
                    return;
                }

                // 카테고리 확인
                JsonNode categoryNode = json.get("category");

                if (categoryNode == null || !categoryNode.isTextual()) {
                    log.warn("카테고리가 없는 요청 등록 이벤트");
                    return;
                }

                RequestCategory category;

                try {
                    category = RequestCategory.valueOf(
                            categoryNode.asText()
                    );
                } catch (IllegalArgumentException e) {
                    log.warn(
                            "지원하지 않는 카테고리: {}",
                            categoryNode.asText()
                    );
                    return;
                }

                saveActionLog(
                        new UserActionLog(
                                eventId,
                                memberId,
                                action,
                                null,
                                requestId,
                                category
                        )
                );

                log.info(
                        "REQUEST_CREATE 저장 - memberId: {}, requestId: {}, category: {}",
                        memberId,
                        requestId,
                        category
                );

                return;
            }

            // 8. VIEW / FAVORITE / OFFER 처리
            if (!"VIEW".equals(action)
                    && !"FAVORITE".equals(action)
                    && !"OFFER".equals(action)) {

                log.warn(
                        "지원하지 않는 행동 유형: {}",
                        action
                );
                return;
            }

            // 개인화 행동은 회원 ID 필수
            if (memberId == null) {
                log.warn("회원 ID가 없는 개인화 행동 이벤트");
                return;
            }

            // 9. 게시글 ID 추출
            JsonNode requestIdNode = json.get("requestId");

            if (requestIdNode == null
                    || !requestIdNode.isIntegralNumber()
                    || !requestIdNode.canConvertToLong()) {

                log.warn("유효하지 않은 게시글 ID");
                return;
            }

            Long requestId = requestIdNode.longValue();

            if (requestId <= 0) {
                log.warn("게시글 ID는 1 이상이어야 합니다.");
                return;
            }

            // 10. 카테고리 추출
            JsonNode categoryNode = json.get("category");

            if (categoryNode == null || !categoryNode.isTextual()) {
                log.warn("카테고리가 없는 행동 이벤트");
                return;
            }

            RequestCategory category;

            try {
                category = RequestCategory.valueOf(
                        categoryNode.asText()
                );
            } catch (IllegalArgumentException e) {
                log.warn(
                        "지원하지 않는 카테고리: {}",
                        categoryNode.asText()
                );
                return;
            }

            // 11. VIEW / FAVORITE / OFFER 로그 저장
            saveActionLog(
                    new UserActionLog(
                            eventId,
                            memberId,
                            action,
                            null,
                            requestId,
                            category
                    )
            );

            log.info(
                    "사용자 행동 저장 - memberId: {}, action: {}, requestId: {}, category: {}",
                    memberId,
                    action,
                    requestId,
                    category
            );

        } catch (JacksonException e) {
            log.warn("Kafka JSON 파싱 실패", e);
        }
    }

    /**
     * 사용자 행동 로그 저장 및 회원 제재 필터링
     *
     * INCLUDED : 개인화 분석 반영
     * EXCLUDED : 개인화 분석 제외
     *
     * EXCLUDED 상태여도 원본 행동 로그는 저장한다.
     */
    private void saveActionLog(UserActionLog actionLog) {

        // 1. 회원 제재 상태 확인
        UserActionFilterDecision decision =
                userActionFilterService.evaluate(
                        actionLog.getMemberId()
                );

        // 2. 필터링 결과 적용
        actionLog.applyFilter(
                decision.result(),
                decision.reason(),
                decision.sanctionId()
        );

        // 3. 행동 로그 및 필터링 결과 저장
        UserActionLog savedLog =
                userActionLogRepository.save(actionLog);

        Long savedLogId = savedLog.getId();

        // 4. DB 트랜잭션 커밋 후 WebSocket 알림 전송
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {
                            userActionWebSocketPublisher.publishCreated(
                                    savedLogId
                            );
                        } catch (Exception e) {
                            log.error(
                                    "관리자 행동 로그 WebSocket 알림 실패 - logId: {}",
                                    savedLogId,
                                    e
                            );
                        }
                    }
                }
        );

        // 5. 처리 결과 로깅
        log.info(
                "Kafka 행동 필터링 완료 - eventId: {}, memberId: {}, action: {}, result: {}, reason: {}, sanctionId: {}",
                actionLog.getEventId(),
                actionLog.getMemberId(),
                actionLog.getAction(),
                decision.result(),
                decision.reason(),
                decision.sanctionId()
        );
    }
}