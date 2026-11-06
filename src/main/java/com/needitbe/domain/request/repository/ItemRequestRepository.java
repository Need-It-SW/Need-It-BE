package com.needitbe.domain.request.repository;

import com.needitbe.domain.request.entity.ItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRequestRepository
        extends JpaRepository<ItemRequest, Long>,
        JpaSpecificationExecutor<ItemRequest> {
    Page<ItemRequest> findByMemberId(
            Long memberId,
            Pageable pageable
    );

}

