package com.needitbe.domain.notification.controller;

import com.needitbe.domain.notification.dto.NotificationResponse;
import com.needitbe.domain.notification.service.NotificationService;
import com.needitbe.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<NotificationResponse> response =
                notificationService.getMyNotifications(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "알림 목록 조회 성공",
                        response
                )
        );
    }

    // 읽지 않은 알림 개수
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        long unreadCount =
                notificationService.getUnreadCount(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "읽지 않은 알림 개수 조회 성공",
                        Map.of("unreadCount", unreadCount)
                )
        );
    }

    // 개별 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        notificationService.markAsRead(
                memberId,
                notificationId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "알림 읽음 처리 성공",
                        null
                )
        );
    }

    // 전체 알림 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        int readCount =
                notificationService.markAllAsRead(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "전체 알림 읽음 처리 성공",
                        Map.of("readCount", readCount)
                )
        );
    }

    // 개별 알림 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        notificationService.deleteNotification(
                memberId,
                notificationId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "알림 삭제 성공",
                        null
                )
        );
    }

    // 전체 알림 삭제
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> deleteAllNotifications(
            Authentication authentication
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        long deletedCount =
                notificationService.deleteAllNotifications(memberId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "전체 알림 삭제 성공",
                        Map.of("deletedCount", deletedCount)
                )
        );
    }
}