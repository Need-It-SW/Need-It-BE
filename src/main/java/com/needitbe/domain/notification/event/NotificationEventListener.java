package com.needitbe.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.SimpUser;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SimpUserRegistry simpUserRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleNotificationCreated(
            NotificationCreatedEvent event
    ) {

        try {

            log.info(
                    "현재 WebSocket 등록 사용자 목록: {}",
                    simpUserRegistry.getUsers()
                            .stream()
                            .map(u -> u.getName())
                            .toList()
            );
            SimpUser user = simpUserRegistry.getUser(

                    event.receiverId().toString()

            );

            if (user == null) {

                log.warn(

                        "❌ WebSocket 사용자 미등록 - receiverId={}",

                        event.receiverId()

                );

            } else {

                log.info(

                        "✅ WebSocket 사용자 등록 확인 - receiverId={}, sessionCount={}",

                        event.receiverId(),

                        user.getSessions().size()

                );

            }
            messagingTemplate.convertAndSendToUser(
                    event.receiverId().toString(),
                    "/queue/notifications",
                    event.notification()
            );

            log.info(
                    "실시간 알림 전송 완료 - receiverId={}, notificationId={}",
                    event.receiverId(),
                    event.notification().notificationId()
            );

        } catch (Exception e) {

            log.error(
                    "실시간 알림 전송 실패 - receiverId={}",
                    event.receiverId(),
                    e
            );
        }
    }
}