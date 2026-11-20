package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdResponse;
import com.needitbe.domain.campaign.entity.AdType;
import com.needitbe.domain.campaign.entity.CampaignBanner;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;
import com.needitbe.domain.member.entity.Member;
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

        List<CampaignBanner> availableAds =
                campaignBanners.stream()
                        .filter(cb ->
                                adImpressionLimitService.canImpress(
                                        cb.getCampaign(),
                                        null
                                )
                        )
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
                        .toList()
        );

        // 6. 사용자별 광고 노출 제한
        List<CampaignBanner> availableAds =
                campaignBanners.stream()
                        .filter(cb ->
                                adImpressionLimitService.canImpress(
                                        cb.getCampaign(),
                                        memberId
                                )
                        )
                        .toList();

        // ==========================================
// 광고 후보 평가 결과 저장
// ==========================================

// 동일 캠페인에 여러 배너가 연결되어 있어도
// 한 번의 광고 조회 요청에서 캠페인당 한 번만 평가
        Map<Long, AdCampaign> uniqueCampaigns = new LinkedHashMap<>();

        for (CampaignBanner cb : campaignBanners) {

            AdCampaign campaign = cb.getCampaign();

            if (campaign.getAdType() == AdType.PERSONALIZED) {
                uniqueCampaigns.putIfAbsent(
                        campaign.getId(),
                        campaign
                );
            }
        }

        for (AdCampaign campaign : uniqueCampaigns.values()) {

            List<AdExclusionReasonType> reasons =
                    evaluateCampaign(
                            campaign,
                            memberId,
                            memberRegion,
                            interestScores,
                            searchKeywords,
                            userActions
                    );

            adFilterEvaluationService.saveEvaluation(
                    campaign.getId(),
                    memberId,
                    reasons
            );
        }

        // ==========================================
        // 디버깅 3. 노출 제한 통과 여부
        // ==========================================

        log.info("노출 제한 통과 캠페인 ID: {}",
                availableAds.stream()
                        .map(cb -> cb.getCampaign().getId())
                        .toList()
        );

        // ==========================================
        // 디버깅 4. 캠페인별 매칭 결과
        // ==========================================

        availableAds.stream()
                .filter(cb ->
                        cb.getCampaign().getAdType()
                                == AdType.PERSONALIZED
                )
                .forEach(cb -> {

                    var campaign = cb.getCampaign();

                    boolean regionMatch = matchesRegion(
                            memberRegion,
                            campaign.getTargetRegions()
                    );

                    boolean keywordMatch = matchesKeywords(
                            searchKeywords,
                            campaign.getTargetKeywords()
                    );

                    String targetCategory =
                            campaign.getTargetCategory();

                    boolean categoryMatch =
                            targetCategory != null
                                    && !targetCategory.isBlank()
                                    && interestScores.keySet()
                                    .stream()
                                    .anyMatch(category ->
                                            targetCategory.trim()
                                                    .equalsIgnoreCase(
                                                            category.name()
                                                    )
                                    );

                    boolean actionMatch = matchesTargetAction(
                            userActions,
                            campaign.getTargetAction(),
                            campaign.getTargetCategory()
                    );

                    log.info("========== 캠페인 {} ==========",
                            campaign.getId());

                    log.info("캠페인 이름: {}", campaign.getName());
                    log.info("광고 유형: {}", campaign.getAdType());
                    log.info("타깃 지역: {}", campaign.getTargetRegions());
                    log.info("타깃 카테고리: {}", targetCategory);
                    log.info("타깃 행동: {}", campaign.getTargetAction());
                    log.info("타깃 키워드: {}", campaign.getTargetKeywords());

                    log.info("지역 매칭: {}", regionMatch);
                    log.info("키워드 매칭: {}", keywordMatch);
                    log.info("카테고리 매칭: {}", categoryMatch);
                    log.info("행동 유형 매칭: {}", actionMatch);

                    log.info("최종 매칭: {}",
                            regionMatch
                                    && keywordMatch
                                    && categoryMatch
                                    && actionMatch
                    );
                });

        // 7. 개인화 광고 필터링 및 우선순위 정렬
        List<CampaignBanner> matchedAds =
                availableAds.stream()

                        .filter(cb ->
                                cb.getCampaign().getAdType()
                                        == AdType.PERSONALIZED
                        )

                        .filter(cb ->
                                matchesRegion(
                                        memberRegion,
                                        cb.getCampaign().getTargetRegions()
                                )
                        )

                        .filter(cb ->
                                matchesKeywords(
                                        searchKeywords,
                                        cb.getCampaign().getTargetKeywords()
                                )
                        )

                        .filter(cb -> {

                            String targetCategory =
                                    cb.getCampaign().getTargetCategory();

                            if (targetCategory == null
                                    || targetCategory.isBlank()) {
                                return false;
                            }

                            return interestScores.keySet()
                                    .stream()
                                    .anyMatch(category ->
                                            targetCategory.trim()
                                                    .equalsIgnoreCase(
                                                            category.name()
                                                    )
                                    );
                        })

                        .filter(cb ->
                                matchesTargetAction(
                                        userActions,
                                        cb.getCampaign().getTargetAction(),
                                        cb.getCampaign().getTargetCategory()
                                )
                        )

                        .sorted(
                                Comparator.comparingDouble(
                                        (CampaignBanner cb) ->
                                                adRankingService.calculateScore(
                                                        cb.getCampaign(),
                                                        userActions,
                                                        interestScores
                                                )
                                ).reversed()
                        )

                        .toList();

        // ==========================================
        // 디버깅 5. 최종 개인화 광고 결과
        // ==========================================

        log.info("최종 개인화 광고 캠페인 ID: {}",
                matchedAds.stream()
                        .map(cb -> cb.getCampaign().getId())
                        .toList()
        );

        // 8. 일반 광고 조회
        List<CampaignBanner> generalAds =
                availableAds.stream()
                        .filter(cb ->
                                cb.getCampaign().getAdType()
                                        == AdType.GENERAL
                        )
                        .filter(cb ->
                                matchesRegion(
                                        memberRegion,
                                        cb.getCampaign().getTargetRegions()
                                )
                        )
                        .toList();

        // 9. 개인화 광고 우선, 없으면 일반 광고
        List<CampaignBanner> selectedAds =
                matchedAds.isEmpty()
                        ? generalAds
                        : matchedAds;

        log.info("최종 반환 캠페인 ID: {}",
                selectedAds.stream()
                        .map(cb -> cb.getCampaign().getId())
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