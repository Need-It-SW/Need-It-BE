package com.needitbe.domain.useraction.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishCreated(Long logId) {

        messagingTemplate.convertAndSend(
                "/topic/admin/user-actions",
                new UserActionChangedMessage(
                        "USER_ACTION_CREATED",
                        logId
                )
        );

        log.debug(
                "관리자 행동 로그 WebSocket 알림 전송 - logId: {}",
                logId
        );
    }

    public record UserActionChangedMessage(
            String type,
            Long logId
    ) {
    }
}