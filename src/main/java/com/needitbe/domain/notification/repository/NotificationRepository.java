package com.needitbe.domain.notification.repository;

import com.needitbe.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // 내 알림 최신순 조회
    List<Notification> findByMemberIdOrderByCreatedAtDesc(
            Long memberId
    );

    // 읽지 않은 알림 개수
    long countByMemberIdAndReadAtIsNull(
            Long memberId
    );

    // 내 알림 단건 조회
    Optional<Notification> findByIdAndMemberId(
            Long notificationId,
            Long memberId
    );

    // 내 알림 전체 조회
    List<Notification> findByMemberId(
            Long memberId
    );

    // 내 알림 전체 삭제
    long deleteByMemberId(
            Long memberId
    );
}