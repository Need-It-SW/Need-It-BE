package com.needitbe.domain.request.repository;

import com.needitbe.domain.request.entity.ItemRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRequestImageRepository
        extends JpaRepository<ItemRequestImage, Long> {

    // 게시글 이미지 목록 조회
    List<ItemRequestImage> findByItemRequestIdOrderBySortOrderAsc(
            Long itemRequestId
    );

    // 특정 게시글에 속한 이미지 조회
    Optional<ItemRequestImage> findByIdAndItemRequestId(
            Long id,
            Long itemRequestId
    );
}