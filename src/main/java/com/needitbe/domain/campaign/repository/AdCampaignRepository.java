package com.needitbe.domain.campaign.repository;

import com.needitbe.domain.campaign.entity.AdCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;




public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AdCampaign c WHERE c.id = :campaignId")
    Optional<AdCampaign> findByIdForUpdate(
            @Param("campaignId") Long campaignId
    );
}