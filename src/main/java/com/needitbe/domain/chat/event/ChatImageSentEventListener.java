package com.needitbe.domain.chat.event;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatImageSentEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleChatImageSent(ChatImageSentEvent event) {

        messagingTemplate.convertAndSend(
                "/topic/chats/" + event.chatRoomId(),
                event.message()
        );
    }
}