package com.needitbe.domain.offer.repository;

import com.needitbe.domain.offer.entity.OfferImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferImageRepository
        extends JpaRepository<OfferImage, Long> {

    // 특정 제안의 이미지 목록 조회
    List<OfferImage> findByOfferIdOrderBySortOrderAsc(
            Long offerId
    );

    // 특정 제안에 속한 이미지 조회
    Optional<OfferImage> findByIdAndOfferId(
            Long imageId,
            Long offerId
    );
}