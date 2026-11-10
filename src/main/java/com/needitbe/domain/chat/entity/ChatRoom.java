package com.needitbe.domain.chat.entity;

import com.needitbe.domain.offer.entity.Offer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_offer",
                        columnNames = "offer_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChatRoom(Offer offer) {
        this.offer = offer;
        this.createdAt = LocalDateTime.now();
    }
}