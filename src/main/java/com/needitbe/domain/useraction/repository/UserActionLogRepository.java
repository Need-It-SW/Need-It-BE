package com.needitbe.domain.useraction.repository;

import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.request.entity.RequestCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserActionLogRepository
        extends JpaRepository<UserActionLog, Long> {

    // 기존 Kafka 이벤트 중복 저장 방지
    boolean existsByEventId(String eventId);

    // 회원별 카테고리 관심도 점수 집계
    @Query("""
            SELECT
                u.category,
                SUM(
                    CASE
                        WHEN u.action = 'VIEW' THEN 2
                        WHEN u.action = 'FAVORITE' THEN 3
                        ELSE 0
                    END
                )
            FROM UserActionLog u
            WHERE u.memberId = :memberId
              AND u.category IS NOT NULL
              AND u.action IN ('VIEW', 'FAVORITE')
            GROUP BY u.category
            ORDER BY
                SUM(
                    CASE
                        WHEN u.action = 'VIEW' THEN 2
                        WHEN u.action = 'FAVORITE' THEN 3
                        ELSE 0
                    END
                ) DESC
            """)
    List<Object[]> findCategoryInterestScores(
            @Param("memberId") Long memberId
    );

    // 회원의 최근 검색 기록 20개 조회
    List<UserActionLog> findTop20ByMemberIdAndActionAndKeywordIsNotNullOrderByCreatedAtDescIdDesc(
            Long memberId,
            String action
    );
}