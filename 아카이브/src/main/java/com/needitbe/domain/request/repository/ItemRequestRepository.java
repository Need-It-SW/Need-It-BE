package com.needitbe.domain.request.repository;

import com.needitbe.domain.request.entity.ItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRequestRepository
        extends JpaRepository<ItemRequest, Long>,
        JpaSpecificationExecutor<ItemRequest> {
    Page<ItemRequest> findByMemberId(
            Long memberId,
            Pageable pageable
    );

    // 동일 요청 게시글에 대한 동시 수락 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ItemRequest r WHERE r.id = :requestId")
    Optional<ItemRequest> findByIdForUpdate(
            @Param("requestId") Long requestId
    );
}

