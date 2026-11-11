package com.needitbe.domain.notification.service;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.notification.dto.NotificationResponse;
import com.needitbe.domain.notification.entity.Notification;
import com.needitbe.domain.notification.entity.NotificationType;
import com.needitbe.domain.notification.repository.NotificationRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.needitbe.domain.notification.event.NotificationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    // 내 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long memberId) {

        return notificationRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // 읽지 않은 알림 개수
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {

        return notificationRepository
                .countByMemberIdAndReadAtIsNull(memberId);
    }

    // 개별 알림 읽음 처리
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {

        Notification notification = notificationRepository
                .findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "알림을 찾을 수 없습니다."
                        )
                );

        notification.markAsRead();
    }

    // 전체 알림 읽음 처리
    @Transactional
    public int markAllAsRead(Long memberId) {

        List<Notification> notifications =
                notificationRepository.findByMemberId(memberId);

        int readCount = 0;

        for (Notification notification : notifications) {

            if (notification.getReadAt() == null) {
                notification.markAsRead();
                readCount++;
            }
        }

        return readCount;
    }

    // 개별 알림 삭제
    @Transactional
    public void deleteNotification(Long memberId, Long notificationId) {

        Notification notification = notificationRepository
                .findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "알림을 찾을 수 없습니다."
                        )
                );

        notificationRepository.delete(notification);
    }

    // 전체 알림 삭제
    @Transactional
    public long deleteAllNotifications(Long memberId) {

        return notificationRepository.deleteByMemberId(memberId);
    }

    // 알림 생성 (다음 단계에서 제안/채팅과 연결)
    @Transactional
    public NotificationResponse createNotification(
            Long receiverId,
            NotificationType type,
            String content,
            Long targetId
    ) {

        // 1. 알림 수신자 조회
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // 2. 알림 생성
        Notification notification = new Notification(
                receiver,
                type,
                content,
                targetId
        );

        // 3. DB 저장
        Notification savedNotification =
                notificationRepository.saveAndFlush(notification);

        // 4. 응답 DTO 생성
        NotificationResponse response =
                NotificationResponse.from(savedNotification);

        // 5. 실시간 알림 이벤트 발행
        eventPublisher.publishEvent(
                new NotificationCreatedEvent(
                        receiverId,
                        response
                )
        );

        return response;
    }
}