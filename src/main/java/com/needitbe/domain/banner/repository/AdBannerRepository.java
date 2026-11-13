package com.needitbe.domain.banner.repository;

import com.needitbe.domain.banner.entity.AdBanner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdBannerRepository extends JpaRepository<AdBanner, Long> {
}