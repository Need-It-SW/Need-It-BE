package com.needitbe.domain.admin.service;

import com.needitbe.domain.admin.dto.DashboardCategoryResponse;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.needitbe.domain.admin.dto.DashboardSummaryResponse;
import com.needitbe.domain.admin.dto.DashboardCampaignMatchResponse;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;

import com.needitbe.domain.useraction.entity.UserActionLog;

import java.util.Arrays;
import java.util.Locale;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AdCampaignRepository adCampaignRepository;
    private final UserActionLogRepository userActionLogRepository;

    @Transactional(readOnly = true)
    public List<DashboardCategoryResponse> getCategoryStatistics() {

        return userActionLogRepository.countLogsByCategory()
                .stream()
                .map(row -> {

                    String category = row[0] == null
                            ? "UNSPECIFIED"
                            : row[0].toString();

                    long count = ((Number) row[1]).longValue();

                    return new DashboardCategoryResponse(
                            category,
                            count
                    );
                })
                .toList();
    }

    private boolean matchesCampaign(
            AdCampaign campaign,
            UserActionLog log
    ) {

        // 개인화 광고만 매칭
        if (campaign.getAdType() != AdType.PERSONALIZED) {
            return false;
        }

        // 카테고리 비교
        String targetCategory = campaign.getTargetCategory();

        if (targetCategory != null
                && !targetCategory.isBlank()
                && !"ALL".equalsIgnoreCase(targetCategory)) {

            if (log.getCategory() == null
                    || !targetCategory.equalsIgnoreCase(
                    log.getCategory().name()
            )) {
                return false;
            }
        }

        // 행동 유형 비교
        String targetAction = campaign.getTargetAction();

        if (targetAction != null
                && !targetAction.isBlank()
                && !"ALL".equalsIgnoreCase(targetAction)) {

            if (!targetAction.equalsIgnoreCase(log.getAction())) {
                return false;
            }
        }

        // 키워드 비교
        String targetKeywords = campaign.getTargetKeywords();

        if (targetKeywords != null && !targetKeywords.isBlank()) {

            if (log.getKeyword() == null
                    || log.getKeyword().isBlank()) {
                return false;
            }

            boolean keywordMatched = Arrays.stream(
                            targetKeywords.split(",")
                    )
                    .map(String::trim)
                    .filter(keyword -> !keyword.isBlank())
                    .anyMatch(keyword ->
                            log.getKeyword()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(
                                            keyword.toLowerCase(Locale.ROOT)
                                    )
                    );

            if (!keywordMatched) {
                return false;
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    public List<DashboardCampaignMatchResponse> getCampaignMatches() {

        List<AdCampaign> campaigns =
                adCampaignRepository.findAllRunningCampaigns();

        List<UserActionLog> logs =
                userActionLogRepository.findAll();

        return campaigns.stream()
                .filter(campaign ->
                        campaign.getAdType() == AdType.PERSONALIZED
                )
                .map(campaign -> {

                    long matchCount = logs.stream()
                            .filter(log ->
                                    matchesCampaign(campaign, log)
                            )
                            .count();

                    return new DashboardCampaignMatchResponse(
                            campaign.getId(),
                            campaign.getName(),
                            campaign.getStatus(),
                            campaign.getTargetCategory(),
                            matchCount
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        long totalActionLogs =
                userActionLogRepository.count();

        long totalCampaigns =
                adCampaignRepository.count();

        List<AdCampaign> runningCampaigns =
                adCampaignRepository.findAllRunningCampaigns();

        List<UserActionLog> logs =
                userActionLogRepository.findAll();

        long matchedActionLogs = logs.stream()
                .filter(log ->
                        runningCampaigns.stream()
                                .anyMatch(campaign ->
                                        matchesCampaign(campaign, log)
                                )
                )
                .count();

        return new DashboardSummaryResponse(
                totalActionLogs,
                totalCampaigns,
                runningCampaigns.size(),
                matchedActionLogs
        );
    }


}