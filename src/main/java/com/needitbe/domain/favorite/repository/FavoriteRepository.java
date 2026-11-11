package com.needitbe.domain.favorite.repository;

import com.needitbe.domain.favorite.entity.Favorite;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberIdAndItemRequestId(
            Long memberId,
            Long itemRequestId
    );

    Optional<Favorite> findByMemberIdAndItemRequestId(
            Long memberId,
            Long itemRequestId
    );

    @EntityGraph(attributePaths = "itemRequest")
    List<Favorite> findByMemberIdOrderByCreatedAtDesc(
            Long memberId
    );

    void deleteByItemRequestId(Long itemRequestId);

    @Query("""
    SELECT f.itemRequest.id
    FROM Favorite f
    WHERE f.member.id = :memberId
      AND f.itemRequest.id IN :requestIds
""")
    List<Long> findFavoriteRequestIds(
            @Param("memberId") Long memberId,
            @Param("requestIds") List<Long> requestIds
    );
}