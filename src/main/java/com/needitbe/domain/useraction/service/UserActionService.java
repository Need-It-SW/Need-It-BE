package com.needitbe.domain.useraction.service;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.kafka.UserActionProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionService {

    private final UserActionProducer userActionProducer;
    private final ObjectMapper objectMapper;

    public void recordCategoryClick(
            Long memberId,
            RequestCategory category
    ) {

        try {
            String eventId = UUID.randomUUID().toString();

            Map<String, Object> event = Map.of(
                    "eventId", eventId,
                    "userId", memberId,
                    "action", "CATEGORY_CLICK",
                    "category", category.name()
            );

            String message =
                    objectMapper.writeValueAsString(event);

            userActionProducer.send(
                    memberId.toString(),
                    message
            );

            log.info(
                    "CATEGORY_CLICK 이벤트 발행 요청 - memberId: {}, category: {}",
                    memberId,
                    category
            );

        } catch (Exception e) {

            log.error(
                    "CATEGORY_CLICK 이벤트 생성 실패 - memberId: {}, category: {}",
                    memberId,
                    category,
                    e
            );
        }
    }
}