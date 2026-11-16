package com.needitbe.domain.useraction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_action_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_action_event_id",
                        columnNames = "event_id"
                )
        }
)
public class UserActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kafka 이벤트 고유 ID
    @Column(name = "event_id", length = 36)
    private String eventId;

    // 회원 ID
    @Column(name = "member_id")
    private Long memberId;

    // 사용자 행동 유형
    @Column(nullable = false, length = 30)
    private String action;

    // 검색어
    @Column(length = 100)
    private String keyword;

    // 생성 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 생성자
    public UserActionLog(
            String eventId,
            Long memberId,
            String action,
            String keyword
    ) {
        this.eventId = eventId;
        this.memberId = memberId;
        this.action = action;
        this.keyword = keyword;
        this.createdAt = LocalDateTime.now();
    }
}
