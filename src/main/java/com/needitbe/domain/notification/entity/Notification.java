package com.needitbe.domain.notification.entity;

import com.needitbe.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notification_member_created",
                        columnList = "member_id, created_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 수신자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 알림 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // 알림 내용
    @Column(nullable = false, length = 255)
    private String content;

    // 연결 대상 ID (제안 ID 또는 채팅방 ID)
    private Long targetId;

    // 알림 생성 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 읽은 시간 (null이면 안 읽음)
    private LocalDateTime readAt;

    public Notification(
            Member member,
            NotificationType type,
            String content,
            Long targetId
    ) {
        this.member = member;
        this.type = type;
        this.content = content;
        this.targetId = targetId;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}