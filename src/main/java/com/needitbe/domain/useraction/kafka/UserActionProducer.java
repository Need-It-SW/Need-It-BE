package com.needitbe.domain.useraction.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProducer {

    private static final String TOPIC = "needit-user-actions";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String userId, String message) {

        kafkaTemplate.send(TOPIC, userId, message)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        log.error("Kafka 메시지 전송 실패: {}", message, ex);
                        return;
                    }

                    log.info(
                            "Kafka 메시지 전송 성공 - topic: {}, partition: {}, offset: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}