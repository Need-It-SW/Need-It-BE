package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.entity.CampaignBanner;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.entity.MemberStatus;
import com.needitbe.domain.member.entity.MemberRole;
import org.springframework.transaction.annotation.Propagation;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.service.UserInterestService;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.needitbe.domain.ads.entity.AdExclusionReasonType;
import com.needitbe.domain.campaign.entity.AdCampaign;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdService {

    private final AdFilterEvaluationService adFilterEvaluationService;
    private final AdRankingService adRankingService;
    private final MemberRepository memberRepository;
    private final AdImpressionLimitService adImpressionLimitService;
    private final UserInterestService userInterestService;
    private final CampaignBannerRepository campaignBannerRepository;
    private final S3Service s3Service;

    /** Kafka 행동 저장 후 개인화 후보를 평가한다. 실제 광고 노출 이벤트는 만들지 않는다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAfterUserAction(Long memberId) {
        if (memberId == null) return;
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null || member.getRole() == MemberRole.ADMIN) return;
        List<AdCampaign> campaigns = campaignBannerRepository.findAll().stream()
                .filter(cb -> cb.getCampaign().getStatus() == CampaignStatus.RUNNING)
                .filter(cb -> cb.getCampaign().getAdType() == AdType.PERSONALIZED)
                .filter(cb -> cb.getBanner().isActive())
                .map(CampaignBanner::getCampaign)
                .collect(java.util.stream.Collectors.toMap(AdCampaign::getId, c -> c,
                        (first, second) -> first, LinkedHashMap::new))
                .values().stream().toList();
        if (campaigns.isEmpty()) return;
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            for (AdCampaign campaign : campaigns) {
                adFilterEvaluationService.saveEvaluation(campaign.getId(), memberId,
                        List.of(AdExclusionReasonType.MEMBER_SUSPENDED));
            }
            return;
        }
        Map<RequestCategory, Long> scores = userInterestService.getCategoryInterestScores(memberId);
        Set<String> keywords = userInterestService.getRecentSearchKeywords(memberId);
        List<UserActionLog> actions = userInterestService.getRecentActions(memberId);
        for (AdCampaign campaign : campaigns) {
            adFilterEvaluationService.saveEvaluation(campaign.getId(), memberId,
                    evaluateCampaign(campaign, memberId, member.getRegion(), scores, keywords, actions));
        }
    }

    // ==========================================
    // 1. 일반 광고 조회
    // ==========================================

    @Transactional(readOnly = true)
    public List<AdResponse> getAds(String placement) {

        List<CampaignBanner> campaignBanners =
                campaignBannerRepository.findByPlacement(placement)
                        .stream()
                        .filter(cb ->
                                cb.getCampaign().getStatus()
                                        == CampaignStatus.RUNNING
                        )
                        .filter(cb ->
                                cb.getCampaign().getAdType()
                                        == AdType.GENERAL
                        )
                        .filter(cb -> cb.getBanner().isActive())
                        .toList();

        Map<Long, Boolean> limits = new LinkedHashMap<>();
        List<CampaignBanner> availableAds = campaignBanners.stream()
                .filter(cb -> limits.computeIfAbsent(cb.getCampaign().getId(),
                        id -> adImpressionLimitService.canImpress(cb.getCampaign(), null)))
                .toList();

        return toAdResponses(availableAds);
    }

    // ==========================================
    // 2. 개인화 광고 조회
    // ==========================================

    @Transactional

    public List<AdResponse> getPersonalizedAds(
            Long memberId,
            String placement

    ) {

        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );
        // 관리자 및 제재 회원은 사용자용 개인화 광고를 제공하지 않는다.
        if (member.getRole() == MemberRole.ADMIN || member.getStatus() == MemberStatus.SUSPENDED) {
            return List.of();
        }

        String memberRegion = member.getRegion();

        // 2. 관심 카테고리 점수
        Map<RequestCategory, Long> interestScores =
                userInterestService.getCategoryInterestScores(memberId);

        // 3. 최근 검색 키워드
        Set<String> searchKeywords =
                userInterestService.getRecentSearchKeywords(memberId);

        // 4. 최근 행동 로그
        List<UserActionLog> userActions =
                userInterestService.getRecentActions(memberId);

        // ==========================================
        // 디버깅 1. 회원 정보 및 관심도
        // ==========================================

        log.info("========== 개인화 광고 디버깅 시작 ==========");
        log.info("회원 ID: {}", memberId);
        log.info("광고 위치: {}", placement);
        log.info("회원 지역: {}", memberRegion);
        log.info("관심 카테고리 점수: {}", interestScores);
        log.info("최근 검색 키워드: {}", searchKeywords);

        userActions.forEach(action ->
                log.info(
                        "행동 로그: action={}, category={}",
                        action.getAction(),
                        action.getCategory()
                )
        );

        // 5. 광고 배너 조회
        List<CampaignBanner> campaignBanners =
                campaignBannerRepository.findByPlacement(placement)
                        .stream()
                        .filter(cb ->
                                cb.getCampaign().getStatus()
                                        == CampaignStatus.RUNNING
                        )
                        .filter(cb ->
                                cb.getBanner().isActive()
                        )
                        .toList();

        // ==========================================
        // 디버깅 2. 캠페인 상태 및 배너 활성화
        // ==========================================

        log.info("조회 조건 통과 캠페인 ID: {}",
                campaignBanners.stream()
                        .map(cb -> cb.getCampaign().getId())
                        .distinct()
                        .toList()
        );

        // 요청 내부에서 캠페인별로 한 번만 계산한다. 배너 목록과 순서는 유지한다.
        Map<Long, AdCampaign> campaigns = new LinkedHashMap<>();
        for (CampaignBanner cb : campaignBanners) {
            campaigns.putIfAbsent(cb.getCampaign().getId(), cb.getCampaign());
        }
        Map<Long, Double> matchedScores = new LinkedHashMap<>();
        Set<Long> generalIds = new java.util.HashSet<>();
        List<Long> availableIds = new ArrayList<>();
        for (AdCampaign campaign : campaigns.values()) {
            Long id = campaign.getId();
            if (!adImpressionLimitService.canImpress(campaign, memberId)) continue;
            availableIds.add(id);
            boolean regionMatch = matchesRegion(memberRegion, campaign.getTargetRegions());
            if (campaign.getAdType() == AdType.GENERAL) {
                if (regionMatch) generalIds.add(id);
                continue;
            }
            if (campaign.getAdType() != AdType.PERSONALIZED) continue;
            boolean keywordMatch = matchesKeywords(searchKeywords, campaign.getTargetKeywords());
            String targetCategory = campaign.getTargetCategory();
            boolean categoryMatch = targetCategory != null && !targetCategory.isBlank()
                    && interestScores.keySet().stream().anyMatch(category ->
                    targetCategory.trim().equalsIgnoreCase(category.name()));
            boolean actionMatch = matchesTargetAction(userActions, campaign.getTargetAction(), targetCategory);
            boolean matched = regionMatch && keywordMatch && categoryMatch && actionMatch;
            log.info("캠페인 {} 매칭: 지역={}, 키워드={}, 카테고리={}, 행동={}, 최종={}",
                    id, regionMatch, keywordMatch, categoryMatch, actionMatch, matched);
            if (matched) {
                matchedScores.put(id, adRankingService.calculateScore(campaign, userActions, interestScores));
            }
        }
        log.info("노출 제한 통과 캠페인 ID: {}", availableIds);
        // 안정 정렬: 동일 점수의 배너는 기존 조회 순서를 유지한다.
        List<CampaignBanner> matchedAds = campaignBanners.stream()
                .filter(cb -> matchedScores.containsKey(cb.getCampaign().getId()))
                .sorted(Comparator.comparingDouble((CampaignBanner cb) ->
                        matchedScores.get(cb.getCampaign().getId())).reversed())
                .toList();
        log.info("최종 개인화 광고 캠페인 ID: {}", matchedAds.stream()
                .map(cb -> cb.getCampaign().getId()).distinct().toList());
        List<CampaignBanner> generalAds = campaignBanners.stream()
                .filter(cb -> generalIds.contains(cb.getCampaign().getId()))
                .toList();
        // 평가 이력은 기존처럼 Kafka 행동 이벤트에서만 저장한다.

        // 9. 개인화 광고 우선, 없으면 일반 광고
        List<CampaignBanner> selectedAds =
                matchedAds.isEmpty()
                        ? generalAds
                        : matchedAds;

        log.info("최종 반환 캠페인 ID: {}",
                selectedAds.stream()
                        .map(cb -> cb.getCampaign().getId())
                        .distinct()
                        .toList()
        );

        log.info("========== 개인화 광고 디버깅 종료 ==========");

        // 10. 광고 응답 반환
        return toAdResponses(selectedAds);
    }

    // ==========================================
    // 3. 공통 광고 응답 변환
    // ==========================================

    private List<AdResponse> toAdResponses(
            List<CampaignBanner> campaignBanners
    ) {

        return campaignBanners.stream()
                .map(cb ->
                        AdResponse.from(
                                cb,
                                s3Service.getPresignedUrl(
                                        cb.getBanner().getImageKey()
                                ),
                                UUID.randomUUID().toString()
                        )
                )
                .toList();
    }

    // ==========================================
    // 4. 회원 활동 지역과 광고 대상 지역 비교
    // ==========================================

    private boolean matchesRegion(
            String memberRegion,
            String targetRegions
    ) {

        if (targetRegions == null || targetRegions.isBlank()) {
            return true;
        }

        if (memberRegion == null || memberRegion.isBlank()) {
            return Arrays.stream(targetRegions.split(","))
                    .map(String::trim)
                    .anyMatch(region ->
                            region.equals("전국")
                                    || region.equals("전체")
                    );
        }

        String normalizedMemberRegion =
                memberRegion.trim().replaceAll("\\s+", " ");

        return Arrays.stream(targetRegions.split(","))
                .map(String::trim)
                .filter(region -> !region.isBlank())
                .anyMatch(region -> {

                    if (region.equals("전국")
                            || region.equals("전체")) {
                        return true;
                    }

                    String normalizedTarget =
                            region.replaceAll("\\s+", " ");

                    if (normalizedMemberRegion.equals(normalizedTarget)) {
                        return true;
                    }

                    return Arrays.asList(
                            normalizedMemberRegion.split(" ")
                    ).contains(normalizedTarget);
                });
    }

    // ==========================================
    // 5. 광고 타깃 키워드와 검색 기록 비교
    // ==========================================

    private boolean matchesKeywords(
            Set<String> searchKeywords,
            String targetKeywords
    ) {

        if (targetKeywords == null || targetKeywords.isBlank()) {
            return true;
        }

        if (searchKeywords == null || searchKeywords.isEmpty()) {
            return false;
        }

        return Arrays.stream(targetKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .map(keyword ->
                        keyword.toLowerCase(Locale.ROOT)
                )
                .anyMatch(searchKeywords::contains);
    }

    // ==========================================
    // 6. 광고 타깃 행동 유형 및 카테고리 비교
    // ==========================================

    private boolean matchesTargetAction(
            List<UserActionLog> userActions,
            String targetAction,
            String targetCategory
    ) {

        if (targetAction == null
                || targetAction.isBlank()
                || "ALL".equalsIgnoreCase(targetAction.trim())) {
            return true;
        }

        if (userActions == null || userActions.isEmpty()) {
            return false;
        }

        String normalizedAction =
                targetAction.trim().toUpperCase(Locale.ROOT);

        if (!Set.of(
                "SEARCH",
                "CATEGORY_CLICK",
                "VIEW",
                "REQUEST_CREATE",
                "FAVORITE",
                "OFFER"
        ).contains(normalizedAction)) {
            return false;
        }

        return userActions.stream()
                .filter(log ->
                        normalizedAction.equalsIgnoreCase(
                                log.getAction()
                        )
                )
                .anyMatch(log -> {

                    if ("SEARCH".equals(normalizedAction)) {
                        return true;
                    }

                    if (targetCategory == null
                            || targetCategory.isBlank()) {
                        return true;
                    }

                    return log.getCategory() != null
                            && targetCategory.trim()
                            .equalsIgnoreCase(
                                    log.getCategory().name()
                            );
                });
    }

    private List<AdExclusionReasonType> evaluateCampaign(
            AdCampaign campaign,
            Long memberId,
            String memberRegion,
            Map<RequestCategory, Long> interestScores,
            Set<String> searchKeywords,
            List<UserActionLog> userActions
    ) {

        List<AdExclusionReasonType> reasons = new ArrayList<>();

        // 1. 지역 조건
        if (!matchesRegion(
                memberRegion,
                campaign.getTargetRegions()
        )) {
            reasons.add(AdExclusionReasonType.REGION);
        }

        // 2. 키워드 조건
        if (!matchesKeywords(
                searchKeywords,
                campaign.getTargetKeywords()
        )) {
            reasons.add(AdExclusionReasonType.KEYWORD);
        }

        // 3. 카테고리 조건
        String targetCategory = campaign.getTargetCategory();

        boolean categoryMatch =
                targetCategory != null
                        && !targetCategory.isBlank()
                        && interestScores.keySet()
                        .stream()
                        .anyMatch(category ->
                                targetCategory.trim()
                                        .equalsIgnoreCase(category.name())
                        );

        if (!categoryMatch) {
            reasons.add(AdExclusionReasonType.CATEGORY);
        }

        // 4. 행동 유형 조건
        if (!matchesTargetAction(
                userActions,
                campaign.getTargetAction(),
                campaign.getTargetCategory()
        )) {
            reasons.add(AdExclusionReasonType.ACTION);
        }

        // 5. 회원별 노출 제한
        if (!adImpressionLimitService.canImpress(
                campaign,
                memberId
        )) {
            reasons.add(AdExclusionReasonType.FATIGUE);
        }

        return reasons;
    }


}