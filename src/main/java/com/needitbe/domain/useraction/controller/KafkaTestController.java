package com.needitbe.domain.useraction.controller;

import com.needitbe.domain.useraction.kafka.UserActionProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/kafka")
public class KafkaTestController {

    private final UserActionProducer userActionProducer;

    @PostMapping
    public ResponseEntity<String> sendTestMessage() {

        String userId = "test-user-1";

        String message = """
                {
                  "userId": "test-user-1",
                  "action": "SEARCH",
                  "keyword": "노트북"
                }
                """;

        userActionProducer.send(userId, message);

        return ResponseEntity.ok("Kafka 메시지 전송 요청 완료");
    }
}