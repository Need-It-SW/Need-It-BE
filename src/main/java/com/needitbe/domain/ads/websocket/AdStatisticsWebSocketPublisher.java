package com.needitbe.domain.ads.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class AdStatisticsWebSocketPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publishAfterCommit(Long campaignId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("광고 통계 이벤트는 트랜잭션 내부에서 발행해야 합니다.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/admin/ad-statistics",
                        new AdStatisticsChanged("AD_STATISTICS_CHANGED", campaignId));
            }
        });
    }

    public record AdStatisticsChanged(String type, Long campaignId) {}
}
