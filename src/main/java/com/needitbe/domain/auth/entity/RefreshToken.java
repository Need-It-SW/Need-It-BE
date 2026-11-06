package com.needitbe.domain.auth.entity;

import com.needitbe.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    public RefreshToken(
            Member member,
            String tokenHash,
            Instant expiresAt
    ) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void update(
            String tokenHash,
            Instant expiresAt
    ) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}