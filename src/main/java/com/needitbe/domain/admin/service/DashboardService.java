package com.needitbe.domain.admin.service;

import com.needitbe.domain.admin.dto.DashboardCategoryResponse;
import com.needitbe.domain.admin.dto.DashboardSummaryResponse;
import com.needitbe.domain.admin.dto.DashboardCampaignMatchResponse;

import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;

import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.entity.UserActionFilterResult;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AdCampaignRepository adCampaignRepository;
    private final UserActionLogRepository userActionLogRepository;


    // ==========================================
    // 1. 카테고리별 행동 통계
    // INCLUDED 이벤트만 집계
    // ==========================================

    @Transactional(readOnly = true)
    public List<DashboardCategoryResponse> getCategoryStatistics() {

        return userActionLogRepository
                .countLogsByCategory(UserActionFilterResult.INCLUDED)
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


    // ==========================================
    // 2. 캠페인 조건 매칭
    // ==========================================

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


    // ==========================================
    // 3. 캠페인별 매칭 통계
    // INCLUDED 이벤트만 사용
    // ==========================================

    @Transactional(readOnly = true)
    public List<DashboardCampaignMatchResponse> getCampaignMatches() {

        List<AdCampaign> campaigns =
                adCampaignRepository.findAllRunningCampaigns();

        // Kafka 필터링을 통과한 정상 이벤트만 조회
        List<UserActionLog> logs =
                userActionLogRepository.findByFilterResult(
                        UserActionFilterResult.INCLUDED
                );

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


    // ==========================================
    // 4. 관리자 대시보드 요약 통계
    // ==========================================

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        // 전체 행동 로그 수
        // INCLUDED / EXCLUDED / NULL 모두 포함
        long totalActionLogs =
                userActionLogRepository.count();

        // 전체 캠페인 수
        long totalCampaigns =
                adCampaignRepository.count();

        // 현재 진행 중인 캠페인
        List<AdCampaign> runningCampaigns =
                adCampaignRepository.findAllRunningCampaigns();

        // 광고 매칭에는 INCLUDED 이벤트만 사용
        List<UserActionLog> logs =
                userActionLogRepository.findByFilterResult(
                        UserActionFilterResult.INCLUDED
                );

        // 진행 중인 개인화 캠페인과 매칭되는 정상 이벤트 수
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