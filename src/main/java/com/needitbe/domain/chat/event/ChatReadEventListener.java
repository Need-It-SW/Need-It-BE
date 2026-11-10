package com.needitbe.domain.chat.event;

import com.needitbe.domain.chat.dto.ChatReadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatReadEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleChatReadCompleted(
            ChatReadCompletedEvent event
    ) {

        ChatReadEvent response = new ChatReadEvent(
                event.chatRoomId(),
                event.readerId(),
                event.readCount(),
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend(
                "/topic/chats/" + event.chatRoomId() + "/read",
                response
        );
    }
}