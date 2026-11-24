package com.needitbe.domain.ads.entity;

public enum AdExclusionReasonType {

    // 회원 지역과 광고 타깃 지역 불일치
    REGION,

    // 관심 카테고리 불일치
    CATEGORY,

    // 검색 키워드 불일치
    KEYWORD,

    // 행동 유형 불일치
    ACTION,

    // 최대 광고 노출 횟수 초과
    FATIGUE,

    // 제재 회원으로 인한 광고 제외
    MEMBER_SUSPENDED,

    // 기타 제외 사유
    OTHER
}