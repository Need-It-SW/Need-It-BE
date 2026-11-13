package com.needitbe.domain.campaign.entity;

import com.needitbe.domain.banner.entity.AdBanner;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "campaign_banners",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_campaign_banner_placement",
                        columnNames = {
                                "campaign_id",
                                "banner_id",
                                "placement"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연결된 광고 캠페인
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    // 연결된 광고 배너
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banner_id", nullable = false)
    private AdBanner banner;

    // 광고 노출 영역
    @Column(nullable = false, length = 50)
    private String placement;

    public CampaignBanner(
            AdCampaign campaign,
            AdBanner banner,
            String placement
    ) {
        this.campaign = campaign;
        this.banner = banner;
        this.placement = placement;
    }
}