package com.needitbe.domain.admin.entity;

import com.needitbe.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_sanctions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제재 대상 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 제재한 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;

    // 제재 사유
    @Column(nullable = false, length = 500)
    private String reason;

    // 제재 시작 시간
    @Column(nullable = false)
    private LocalDateTime startedAt;

    // 제재 종료 예정 시간 (null이면 무기한)
    private LocalDateTime expiresAt;

    // 실제 제재 해제 시간
    private LocalDateTime releasedAt;

    public MemberSanction(
            Member member,
            Member admin,
            String reason,
            LocalDateTime expiresAt
    ) {
        this.member = member;
        this.admin = admin;
        this.reason = reason;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    // 제재 해제
    public void release() {
        if (this.releasedAt != null) {
            throw new IllegalStateException("이미 해제된 제재입니다.");
        }

        this.releasedAt = LocalDateTime.now();
    }

    // 현재 제재가 유효한지 확인
    public boolean isActive() {
        if (releasedAt != null) {
            return false;
        }

        return expiresAt == null
                || LocalDateTime.now().isBefore(expiresAt);
    }

    // 제재 기간이 만료되었는지 확인
    public boolean isExpired() {

        return releasedAt == null
                && expiresAt != null
                && !LocalDateTime.now().isBefore(expiresAt);
    }
}