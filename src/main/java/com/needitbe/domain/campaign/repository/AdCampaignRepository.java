package com.needitbe.domain.campaign.repository;

import com.needitbe.domain.campaign.entity.AdCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {
}