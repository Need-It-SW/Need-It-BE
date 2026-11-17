package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.dto.AdResponse;

import com.needitbe.domain.campaign.entity.AdType;

import com.needitbe.domain.campaign.entity.CampaignBanner;

import com.needitbe.domain.campaign.entity.CampaignStatus;

import com.needitbe.domain.campaign.repository.CampaignBannerRepository;

import com.needitbe.domain.request.entity.RequestCategory;

import com.needitbe.domain.useraction.service.UserInterestService;

import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

@Service

@RequiredArgsConstructor

public class AdService {

    private final MemberRepository memberRepository;

    private final AdImpressionLimitService adImpressionLimitService;

    private final UserInterestService userInterestService;

    private final CampaignBannerRepository campaignBannerRepository;

    private final S3Service s3Service;

    // 일반 광고 조회

    @Transactional(readOnly = true)

    public List<AdResponse> getAds(String placement) {

        // 1. 해당 영역의 광고 조회

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

        // 2. 노출 한도 확인

        List<CampaignBanner> availableAds =

                campaignBanners.stream()

                        .filter(cb ->
                                adImpressionLimitService.canImpress(
                                        cb.getCampaign(),
                                        null
                                )
                        )

                        .toList();

        // 3. 광고 응답 반환

        return toAdResponses(availableAds);

    }

    // 개인화 광고 조회

    @Transactional(readOnly = true)
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

        // 2. 회원 관심 카테고리 조회
        Map<RequestCategory, Long> interestScores =
                userInterestService.getCategoryInterestScores(memberId);

        // 회원 최근 검색 키워드
        Set<String> searchKeywords =
                userInterestService.getRecentSearchKeywords(memberId);

        // 3. 광고 배너 조회
        List<CampaignBanner> campaignBanners =
                campaignBannerRepository.findByPlacement(placement)
                        .stream()
                        .filter(cb ->
                                cb.getCampaign().getStatus()
                                        == CampaignStatus.RUNNING
                        )
                        .filter(cb -> cb.getBanner().isActive())
                        .toList();

        // 4. 노출 한도 확인
        List<CampaignBanner> availableAds =
                campaignBanners.stream()
                        .filter(cb ->
                                adImpressionLimitService.canImpress(
                                        cb.getCampaign(),
                                        memberId
                                )
                        )
                        .toList();

        // 5. 개인화 광고 필터링
        List<CampaignBanner> matchedAds =
                availableAds.stream()

                        // 개인화 광고만
                        .filter(cb ->
                                cb.getCampaign().getAdType()
                                        == AdType.PERSONALIZED
                        )

                        // 지역 매칭
                        .filter(cb ->
                                matchesRegion(
                                        memberRegion,
                                        cb.getCampaign().getTargetRegions()
                                )
                        )

                        // 검색 키워드 매칭
                        .filter(cb ->
                                matchesKeywords(
                                        searchKeywords,
                                        cb.getCampaign().getTargetKeywords()
                                )
                        )

                        // 관심 카테고리 매칭
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

                        // 관심도 높은 카테고리 우선
                        .sorted(
                                Comparator.comparingLong(
                                        (CampaignBanner cb) -> {

                                            String targetCategory =
                                                    cb.getCampaign()
                                                            .getTargetCategory();

                                            return interestScores.entrySet()
                                                    .stream()
                                                    .filter(entry ->
                                                            entry.getKey()
                                                                    .name()
                                                                    .equalsIgnoreCase(
                                                                            targetCategory.trim()
                                                                    )
                                                    )
                                                    .mapToLong(
                                                            Map.Entry::getValue
                                                    )
                                                    .findFirst()
                                                    .orElse(0L);
                                        }
                                ).reversed()
                        )
                        .toList();

        // 6. 일반 광고
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

        // 7. 개인화 광고 우선, 없으면 일반 광고
        List<CampaignBanner> selectedAds =
                matchedAds.isEmpty()
                        ? generalAds
                        : matchedAds;

        // 8. 응답 반환
        return toAdResponses(selectedAds);
    }

    // 공통 광고 응답 변환

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
    // 회원 활동 지역과 광고 대상 지역 비교
    private boolean matchesRegion(
            String memberRegion,
            String targetRegions
    ) {

        // 광고 대상 지역을 설정하지 않았다면 전체 지역 허용
        if (targetRegions == null || targetRegions.isBlank()) {
            return true;
        }

        // 전체 지역 광고
        if (targetRegions.trim().equals("전국")
                || targetRegions.trim().equals("전체")) {
            return true;
        }

        // 회원 지역이 없다면 지역 광고 매칭 불가
        if (memberRegion == null || memberRegion.isBlank()) {
            return false;
        }

        // 쉼표로 구분된 지역 목록 확인
        return Arrays.stream(targetRegions.split(","))
                .map(String::trim)
                .anyMatch(region ->
                        region.equals(memberRegion.trim())
                                || region.equals("전국")
                                || region.equals("전체")
                );
    }

    // 광고 타깃 키워드와 회원 검색 기록 비교
    private boolean matchesKeywords(
            Set<String> searchKeywords,
            String targetKeywords
    ) {

        // 타깃 키워드가 없으면 제한하지 않음
        if (targetKeywords == null || targetKeywords.isBlank()) {
            return true;
        }

        if (searchKeywords == null || searchKeywords.isEmpty()) {
            return false;
        }

        return Arrays.stream(targetKeywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(searchKeywords::contains);
    }


}