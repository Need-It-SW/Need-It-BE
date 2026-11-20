package com.needitbe.domain.useraction.service;

import com.needitbe.domain.admin.dto.AdminUserActionStatisticsResponse;
import com.needitbe.domain.campaign.entity.AdCampaign;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.dto.response.AdminUserActionResponse;
import com.needitbe.domain.useraction.dto.response.AdminUserActionResponse.MatchedCampaignResponse;
import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.repository.UserActionLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserActionService {

    private final UserActionLogRepository userActionLogRepository;
    private final MemberRepository memberRepository;
    private final AdCampaignRepository adCampaignRepository;

    public Page<AdminUserActionResponse> getLogs(
            String action,
            RequestCategory category,
            String keyword,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {

        if (startDate != null
                && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        LocalDateTime startDateTime =
                startDate == null ? null : startDate.atStartOfDay();

        LocalDateTime endDateTime =
                endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        String normalizedAction =
                action == null || action.isBlank()
                        ? null
                        : action.trim().toUpperCase(Locale.ROOT);

        String normalizedKeyword =
                keyword == null || keyword.isBlank()
                        ? null
                        : keyword.trim();

        String normalizedSearch = normalizeSearch(search);

        // 1. 행동 로그 조회
        Page<UserActionLog> logs =
                userActionLogRepository.searchAdminLogs(
                        normalizedAction,
                        category,
                        normalizedKeyword,
                        startDateTime,
                        endDateTime,
                        normalizedSearch,
                        pageable
                );

        // 2. 해당 페이지 회원 ID 수집
        Set<Long> memberIds = logs.getContent()
                .stream()
                .map(UserActionLog::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 회원 정보 일괄 조회
        Map<Long, Member> memberMap =
                memberRepository.findAllById(memberIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Member::getId,
                                Function.identity()
                        ));

        // 4. 현재 진행 중인 개인화 캠페인 조회
        List<AdCampaign> campaigns =
                adCampaignRepository.findRunningCampaignsByAdType(
                        AdType.PERSONALIZED
                );

        // 5. 로그별 매칭 캠페인 계산
        return logs.map(log -> {

            Member member = memberMap.get(log.getMemberId());

            String region =
                    member == null ? null : member.getRegion();

            List<MatchedCampaignResponse> matchedCampaigns =
                    campaigns.stream()
                            .filter(campaign ->
                                    matchesCampaign(log, region, campaign)
                            )
                            .map(campaign ->
                                    new MatchedCampaignResponse(
                                            campaign.getId(),
                                            campaign.getName()
                                    )
                            )
                            .toList();

            return AdminUserActionResponse.from(
                    log,
                    region,
                    matchedCampaigns
            );
        });
    }

    // ==========================================
    // 개별 행동 로그와 캠페인 조건 비교
    // ==========================================

    private boolean matchesCampaign(
            UserActionLog log,
            String memberRegion,
            AdCampaign campaign
    ) {

        // 1. 진행 중인 개인화 캠페인인지 확인
        if (campaign.getStatus() != CampaignStatus.RUNNING
                || campaign.getAdType() != AdType.PERSONALIZED) {
            return false;
        }

        // 2. 지역 조건 확인
        if (!matchesRegion(
                memberRegion,
                campaign.getTargetRegions()
        )) {
            return false;
        }

        // 3. 행동 유형 확인
        if (!matchesAction(
                log.getAction(),
                campaign.getTargetAction()
        )) {
            return false;
        }

        // 4. 카테고리 조건 확인
        if (!matchesCategory(
                log,
                campaign.getTargetCategory()
        )) {
            return false;
        }

        // 5. 검색 키워드 조건 확인
        return matchesKeyword(
                log,
                campaign.getTargetKeywords()
        );
    }

    private boolean matchesAction(
            String logAction,
            String targetAction
    ) {

        if (targetAction == null
                || targetAction.isBlank()
                || "ALL".equalsIgnoreCase(targetAction.trim())) {
            return true;
        }

        if (logAction == null) {
            return false;
        }

        return targetAction.trim()
                .equalsIgnoreCase(logAction.trim());
    }

    private boolean matchesCategory(
            UserActionLog log,
            String targetCategory
    ) {

        if (targetCategory == null
                || targetCategory.isBlank()
                || "ALL".equalsIgnoreCase(targetCategory.trim())) {
            return true;
        }

        // SEARCH 로그에는 카테고리가 없을 수 있음
        if (log.getCategory() == null) {
            return false;
        }

        return targetCategory.trim()
                .equalsIgnoreCase(log.getCategory().name());
    }

    private boolean matchesKeyword(
            UserActionLog log,
            String targetKeywords
    ) {

        if (targetKeywords == null
                || targetKeywords.isBlank()) {
            return true;
        }

        // 검색 행동이 아니면 키워드 비교 불가
        if (!"SEARCH".equalsIgnoreCase(log.getAction())
                || log.getKeyword() == null
                || log.getKeyword().isBlank()) {
            return false;
        }

        String logKeyword =
                log.getKeyword().trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(targetKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(logKeyword::equals);
    }

    private boolean matchesRegion(
            String memberRegion,
            String targetRegions
    ) {

        // 타깃 지역 미설정 → 전체 지역 허용
        if (targetRegions == null || targetRegions.isBlank()) {
            return true;
        }

        // 회원 지역 미설정
        if (memberRegion == null || memberRegion.isBlank()) {
            return Arrays.stream(targetRegions.split(","))
                    .map(String::trim)
                    .anyMatch(region ->
                            region.equals("전국") || region.equals("전체")
                    );
        }

        String normalizedMemberRegion =
                memberRegion.trim().replaceAll("\\s+", " ");

        return Arrays.stream(targetRegions.split(","))
                .map(String::trim)
                .filter(region -> !region.isBlank())
                .anyMatch(region -> {

                    // 전국 또는 전체
                    if (region.equals("전국") || region.equals("전체")) {
                        return true;
                    }

                    String normalizedTarget =
                            region.replaceAll("\\s+", " ");

                    // 정확히 일치
                    if (normalizedMemberRegion.equals(normalizedTarget)) {
                        return true;
                    }

                    // 공백으로 구분된 지역명 단위로 부분 일치
                    return Arrays.asList(
                            normalizedMemberRegion.split(" ")
                    ).contains(normalizedTarget);
                });
    }

    private String normalizeSearch(String search) {

        if (search == null || search.isBlank()) {
            return null;
        }

        String value = search.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "전자기기" -> "digital";
            case "가구" -> "living";
            case "조회" -> "view";
            case "검색" -> "search";
            default -> value;
        };
    }

    @Transactional(readOnly = true)
    public AdminUserActionStatisticsResponse getStatistics() {

        List<Object[]> results =
                userActionLogRepository.countByActionType();

        List<AdminUserActionStatisticsResponse.ActionCount> counts =
                results.stream()
                        .map(row ->
                                new AdminUserActionStatisticsResponse.ActionCount(
                                        String.valueOf(row[0]),
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        long totalCount = counts.stream()
                .mapToLong(AdminUserActionStatisticsResponse.ActionCount::count)
                .sum();

        return new AdminUserActionStatisticsResponse(
                totalCount,
                counts
        );
    }
}