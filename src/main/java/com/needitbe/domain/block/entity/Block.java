package com.needitbe.domain.block.entity;

import com.needitbe.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_blocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_blocks",
                        columnNames = {"blocker_id", "blocked_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 차단한 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private Member blocker;

    // 차단당한 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private Member blocked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Block(Member blocker, Member blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = LocalDateTime.now();
    }
}