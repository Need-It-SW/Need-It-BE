package com.needitbe.domain.favorite.entity;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.request.entity.ItemRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_favorite_member_request",
                        columnNames = {"member_id", "item_request_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_id",
            nullable = false
    )
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "item_request_id",
            nullable = false
    )
    private ItemRequest itemRequest;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public Favorite(
            Member member,
            ItemRequest itemRequest
    ) {
        this.member = member;
        this.itemRequest = itemRequest;
        this.createdAt = LocalDateTime.now();
    }
}