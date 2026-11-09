package com.needitbe.domain.offer.repository;

import com.needitbe.domain.offer.entity.OfferAcceptanceCancellation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferAcceptanceCancellationRepository
        extends JpaRepository<OfferAcceptanceCancellation, Long> {
}