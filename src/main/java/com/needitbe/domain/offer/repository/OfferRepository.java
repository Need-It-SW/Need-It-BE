package com.needitbe.domain.offer.repository;

import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    // 로그인한 사용자가 보낸 제안 목록
    @EntityGraph(attributePaths = {"itemRequest"})
    List<Offer> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"itemRequest", "member"})
    List<Offer> findByItemRequestMemberIdOrderByCreatedAtDesc(Long memberId);

    // 특정 요청 게시글에 이미 수락된 제안이 있는지 확인
    boolean existsByItemRequestIdAndStatus(
            Long requestId,
            OfferStatus status
    );

    @Query("SELECT o.itemRequest.id FROM Offer o WHERE o.id = :offerId")
    Optional<Long> findRequestIdByOfferId(
            @Param("offerId") Long offerId
    );

    boolean existsByItemRequestId(Long requestId);

}