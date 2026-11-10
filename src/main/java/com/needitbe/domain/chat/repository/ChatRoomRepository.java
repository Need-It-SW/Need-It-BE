package com.needitbe.domain.chat.repository;

import com.needitbe.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 제안에 연결된 채팅방 조회
    Optional<ChatRoom> findByOfferId(Long offerId);

    // 특정 제안의 채팅방 존재 여부
    boolean existsByOfferId(Long offerId);

    @Query("""
    SELECT cr FROM ChatRoom cr
    JOIN FETCH cr.offer o
    JOIN FETCH o.member
    JOIN FETCH o.itemRequest r
    JOIN FETCH r.member
    WHERE o.member.id = :memberId
       OR r.member.id = :memberId
    ORDER BY cr.createdAt DESC
""")
    List<ChatRoom> findMyChatRooms(@Param("memberId") Long memberId);
}