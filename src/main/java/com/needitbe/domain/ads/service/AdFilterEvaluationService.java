package com.needitbe.domain.ads.service;

import com.needitbe.domain.ads.entity.*;
import com.needitbe.domain.ads.repository.AdFilterEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdFilterEvaluationService {

    private final AdFilterEvaluationRepository evaluationRepository;

    @Transactional
    public void saveEvaluation(
            Long campaignId,
            Long memberId,
            List<AdExclusionReasonType> reasons
    ) {

        AdFilterResult result = reasons.isEmpty()
                ? AdFilterResult.SELECTED
                : AdFilterResult.EXCLUDED;

        AdFilterEvaluation evaluation =
                new AdFilterEvaluation(
                        campaignId,
                        memberId,
                        result
                );

        for (AdExclusionReasonType reason : reasons) {

            evaluation.addExclusionReason(
                    reason,
                    getReasonDetail(reason)
            );
        }

        evaluationRepository.save(evaluation);
    }

    private String getReasonDetail(AdExclusionReasonType reason) {

        return switch (reason) {
            case REGION -> "회원 활동 지역과 캠페인 대상 지역 불일치";
            case CATEGORY -> "회원 관심 카테고리와 캠페인 대상 카테고리 불일치";
            case KEYWORD -> "회원 검색 기록과 캠페인 타깃 키워드 불일치";
            case ACTION -> "회원 행동 기록과 캠페인 대상 행동 유형 불일치";
            case FATIGUE -> "회원별 광고 최대 노출 횟수 초과";
            case OTHER -> "기타 광고 후보 제외 사유";
        };
    }
}