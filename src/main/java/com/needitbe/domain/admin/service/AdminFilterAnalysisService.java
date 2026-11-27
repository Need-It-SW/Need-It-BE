package com.needitbe.domain.admin.service;

import com.needitbe.domain.admin.dto.FilterAnalysisSummaryResponse;
import com.needitbe.domain.admin.dto.FilterExclusionResponse;
import com.needitbe.domain.admin.dto.FilterReasonResponse;
import com.needitbe.domain.ads.entity.AdExclusionReasonType;
import com.needitbe.domain.ads.entity.AdFilterEvaluation;
import com.needitbe.domain.ads.entity.AdFilterResult;
import com.needitbe.domain.ads.repository.AdFilterEvaluationRepository;
import com.needitbe.domain.ads.repository.AdFilterExclusionReasonRepository;
import com.needitbe.domain.campaign.repository.AdCampaignRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.needitbe.domain.admin.dto.FilterAdEventResponse;
import com.needitbe.domain.ads.entity.AdEvent;
import com.needitbe.domain.ads.entity.AdEventType;
import com.needitbe.domain.ads.repository.AdEventRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFilterAnalysisService {

    private final AdEventRepository adEventRepository;
    private final AdFilterEvaluationRepository evaluationRepository;
    private final AdFilterExclusionReasonRepository exclusionReasonRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final com.needitbe.domain.member.repository.MemberRepository memberRepository;

    public List<java.util.Map<String, Object>> getCampaignBreakdown(String period) {
        java.util.Map<Long, long[]> counts = new java.util.LinkedHashMap<>();
        for (Object[] row : evaluationRepository.countByCampaignAndResult(resolveStartDate(period))) {
            long[] c = counts.computeIfAbsent((Long) row[0], k -> new long[2]);
            c[row[1] == AdFilterResult.SELECTED ? 0 : 1] += ((Number) row[2]).longValue();
        }
        return counts.entrySet().stream().map(entry -> {
            long id = entry.getKey(); long[] c = entry.getValue();
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("campaignId", id);
            row.put("campaignName", adCampaignRepository.findById(id).map(a -> a.getName()).orElse("캠페인 #" + id));
            row.put("selectedCount", c[0]); row.put("excludedCount", c[1]);
            row.put("totalEvaluations", c[0]+c[1]);
            return row;
        }).toList();
    }

    // ==========================================
    // 1. 고객 필터링 요약 통계
    // ==========================================

    public FilterAnalysisSummaryResponse getSummary(
            Long campaignId,
            String period
    ) {


        LocalDateTime start = resolveStartDate(period);

        long total = evaluationRepository.countEvaluations(campaignId, start);
        long selected = evaluationRepository.countEvaluationsByResult(campaignId, AdFilterResult.SELECTED, start);
        long excluded = evaluationRepository.countEvaluationsByResult(campaignId, AdFilterResult.EXCLUDED, start);

        double selectionRate = total == 0
                ? 0.0
                : Math.round((selected * 10000.0) / total) / 100.0;

        return new FilterAnalysisSummaryResponse(
                campaignId,
                total,
                selected,
                excluded,
                selectionRate
        );
    }

    // ==========================================
    // 2. 제외 사유별 통계
    // ==========================================

    public List<FilterReasonResponse> getReasons(
            Long campaignId,
            String period
    ) {


        LocalDateTime start = resolveStartDate(period);

        List<Object[]> results = exclusionReasonRepository.countReasons(campaignId, start);

        return results.stream()
                .map(row -> new FilterReasonResponse(
                        (AdExclusionReasonType) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // ==========================================
    // 3. 기간 계산
    // ==========================================

    private LocalDateTime resolveStartDate(String period) {

        if (period == null || period.isBlank()) {
            return LocalDateTime.now().minusDays(7);
        }

        LocalDateTime now = LocalDateTime.now();

        return switch (period.trim().toUpperCase()) {

            case "24H" -> now.minusHours(24);

            case "7D" -> now.minusDays(7);

            case "30D" -> now.minusDays(30);

            case "ALL" -> null;

            default -> throw new IllegalArgumentException(
                    "지원하지 않는 분석 기간: " + period
            );
        };
    }

    // ==========================================
    // 4. 캠페인 존재 여부 확인
    // ==========================================

    private void validateCampaign(Long campaignId) {

        if (campaignId != null && !adCampaignRepository.existsById(campaignId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 캠페인입니다. ID: " + campaignId
            );
        }
    }

    public Page<java.util.Map<String, Object>> getEvaluations(String period, Long campaignId, AdFilterResult result, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        return evaluationRepository.findRecentEvaluations(campaignId, result, resolveStartDate(period), PageRequest.of(page, size))
                .map(e -> {
                    java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("id", e.getId());
                    row.put("campaignId", e.getCampaignId());
                    row.put("memberId", e.getMemberId());
                    row.put("nickname", memberRepository.findById(e.getMemberId()).map(m -> m.getNickname()).orElse("회원 #" + e.getMemberId()));
                    row.put("campaignName", adCampaignRepository.findById(e.getCampaignId()).map(c -> c.getName()).orElse("캠페인 #" + e.getCampaignId()));
                    row.put("createdAt", e.getCreatedAt());
                    row.put("result", e.getResult());
                    row.put("reasons", e.getExclusionReasons().stream().map(r -> new FilterExclusionResponse.ExclusionReasonDetail(r.getReason(), r.getDetail())).toList());
                    return row;
                });
    }

    public Page<FilterExclusionResponse> getExclusions(
            Long campaignId,
            String period,
            AdExclusionReasonType reason,
            int page,
            int size
    ) {

        validateCampaign(campaignId);

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "page는 0 이상, size는 1~100이어야 합니다."
            );
        }

        LocalDateTime start = resolveStartDate(period);

        Pageable pageable = PageRequest.of(page, size);

        Page<AdFilterEvaluation> evaluations =
                evaluationRepository.findExclusions(
                        campaignId,
                        start,
                        reason,
                        pageable
                );

        return evaluations.map(evaluation -> {

            List<FilterExclusionResponse.ExclusionReasonDetail> reasons =
                    evaluation.getExclusionReasons()
                            .stream()
                            .map(r ->
                                    new FilterExclusionResponse.ExclusionReasonDetail(
                                            r.getReason(),
                                            r.getDetail()
                                    )
                            )
                            .toList();

            return new FilterExclusionResponse(
                    evaluation.getId(),
                    evaluation.getCampaignId(),
                    evaluation.getMemberId(),
                    evaluation.getCreatedAt(),
                    reasons
            );
        });
    }

    public Page<FilterAdEventResponse> getAdEvents(
            Long campaignId,
            String period,
            AdEventType eventType,
            int page,
            int size
    ) {

        validateCampaign(campaignId);

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "page는 0 이상, size는 1~100이어야 합니다."
            );
        }

        LocalDateTime start = resolveStartDate(period);

        Page<AdEvent> events =
                adEventRepository.findAdminAdEvents(
                        campaignId,
                        start,
                        eventType,
                        PageRequest.of(page, size)
                );

        return events.map(event ->
                new FilterAdEventResponse(
                        event.getId(),
                        event.getCampaignId(),
                        event.getBannerId(),
                        event.getMemberId(),
                        event.getEventType(),
                        event.getImpressionId(),
                        event.getPlacement(),
                        event.getCreatedAt()
                )
        );
    }
}