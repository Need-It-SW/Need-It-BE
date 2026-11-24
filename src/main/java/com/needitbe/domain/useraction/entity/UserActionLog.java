package com.needitbe.domain.useraction.entity;

import com.needitbe.domain.request.entity.RequestCategory;
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

    // 게시글 ID
    @Column(name = "request_id")
    private Long requestId;

    // 게시글 카테고리
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_result", length = 20)
    private UserActionFilterResult filterResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_reason", length = 50)
    private UserActionFilterReason filterReason;

    @Column(name = "sanction_id")
    private Long sanctionId;

    @Column(name = "filtered_at")
    private LocalDateTime filteredAt;public void applyFilter(
            UserActionFilterResult result,
            UserActionFilterReason reason,
            Long sanctionId
    ) {
        this.filterResult = result;
        this.filterReason = reason;
        this.sanctionId = sanctionId;
        this.filteredAt = LocalDateTime.now();
    }

    // 생성 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 기존 SEARCH 이벤트 생성자
    public UserActionLog(
            String eventId,
            Long memberId,
            String action,
            String keyword
    ) {
        this(
                eventId,
                memberId,
                action,
                keyword,
                null,
                null
        );
    }

    // 사용자 행동 이벤트 생성자
    public UserActionLog(
            String eventId,
            Long memberId,
            String action,
            String keyword,
            Long requestId,
            RequestCategory category
    ) {
        this.eventId = eventId;
        this.memberId = memberId;
        this.action = action;
        this.keyword = keyword;
        this.requestId = requestId;
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }



}