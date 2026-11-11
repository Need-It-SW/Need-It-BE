package com.needitbe.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketSessionEventLogger {

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        log.info(
                "WebSocket 세션 연결 - user={}, sessionId={}",
                event.getUser() != null
                        ? event.getUser().getName()
                        : "null",
                accessor.getSessionId()
        );
    }

    @EventListener
    public void handleDisconnected(SessionDisconnectEvent event) {

        log.info(
                "WebSocket 세션 종료 - user={}, sessionId={}",
                event.getUser() != null
                        ? event.getUser().getName()
                        : "null",
                event.getSessionId()
        );
    }
}