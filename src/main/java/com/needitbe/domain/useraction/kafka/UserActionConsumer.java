package com.needitbe.domain.useraction.kafka;

import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final ObjectMapper objectMapper;
    private final UserActionLogRepository userActionLogRepository;

    @KafkaListener(
            topics = "needit-user-actions",
            groupId = "needit-user-actions-group"
    )
    @Transactional
    public void consume(String message) {

        try {
            // 1. JSON 파싱
            JsonNode json = objectMapper.readTree(message);

            // 2. eventId 추출
            JsonNode eventIdNode = json.get("eventId");

            if (eventIdNode == null || !eventIdNode.isTextual()) {
                log.warn("eventId가 없는 이벤트 - 저장 제외");
                return;
            }

            String eventId = eventIdNode.asText();

            // UUID 형식 검증
            try {
                UUID.fromString(eventId);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 eventId 형식 - 저장 제외");
                return;
            }

            // 3. 중복 이벤트 확인
            if (userActionLogRepository.existsByEventId(eventId)) {
                log.info("이미 처리한 Kafka 이벤트 - eventId: {}", eventId);
                return;
            }

            // 4. 사용자 ID 추출
            Long memberId = null;

            JsonNode userIdNode = json.get("userId");

            if (userIdNode != null && !userIdNode.isNull()) {

                if (!userIdNode.isIntegralNumber()
                        || !userIdNode.canConvertToLong()) {

                    log.warn("잘못된 사용자 ID 형식 - 저장 제외");
                    return;
                }

                memberId = userIdNode.longValue();
            }

            // 5. 행동 유형 추출
            JsonNode actionNode = json.get("action");

            if (actionNode == null || !actionNode.isTextual()) {
                log.warn("행동 유형이 없는 Kafka 메시지");
                return;
            }

            String action = actionNode.asText();

            // 현재는 검색 이벤트만 저장
            if (!"SEARCH".equals(action)) {
                log.warn("지원하지 않는 사용자 행동 유형: {}", action);
                return;
            }

            // 6. 검색어 추출
            JsonNode keywordNode = json.get("keyword");

            if (keywordNode == null || !keywordNode.isTextual()) {
                log.warn("검색어가 없는 Kafka 메시지");
                return;
            }

            String keyword = keywordNode.asText();

            if (keyword.isBlank() || keyword.length() > 100) {
                log.warn("유효하지 않은 검색어");
                return;
            }

            // 7. DB 저장
            UserActionLog userActionLog = new UserActionLog(
                    eventId,
                    memberId,
                    action,
                    keyword
            );

            userActionLogRepository.save(userActionLog);

            log.info(
                    "사용자 행동 로그 저장 요청 - eventId: {}, memberId: {}, action: {}",
                    eventId,
                    memberId,
                    action
            );

        } catch (JacksonException e) {
            log.warn("Kafka JSON 파싱 실패 - 이벤트 저장 제외", e);
        }
    }
}