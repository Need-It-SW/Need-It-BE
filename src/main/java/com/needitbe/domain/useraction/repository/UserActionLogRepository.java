package com.needitbe.domain.useraction.repository;

import com.needitbe.domain.useraction.entity.UserActionLog;
import com.needitbe.domain.useraction.entity.UserActionFilterReason;
import com.needitbe.domain.useraction.entity.UserActionFilterResult;
import com.needitbe.domain.request.entity.RequestCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActionLogRepository
        extends JpaRepository<UserActionLog, Long> {

    // ==========================================
    // 1. Kafka 이벤트 중복 저장 방지
    // ==========================================

    boolean existsByEventId(String eventId);


    // ==========================================
    // 2. 회원별 카테고리 관심도 점수 집계
    // ==========================================

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
          AND u.filterResult = :filterResult
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
            @Param("memberId") Long memberId,
            @Param("filterResult") UserActionFilterResult filterResult
    );


    // ==========================================
    // 3. 최근 검색 기록
    // ==========================================

    List<UserActionLog>
    findTop20ByMemberIdAndActionAndKeywordIsNotNullAndFilterResultOrderByCreatedAtDescIdDesc(
            Long memberId,
            String action,
            UserActionFilterResult filterResult
    );


    // ==========================================
    // 4. 최근 행동 로그
    // ==========================================

    List<UserActionLog>
    findTop100ByMemberIdAndFilterResultOrderByCreatedAtDescIdDesc(
            Long memberId,
            UserActionFilterResult filterResult
    );


    // ==========================================
    // 5. 관리자 행동 로그 검색 및 필터링
    // ==========================================

    @Query(
            value = """
                SELECT u
                FROM UserActionLog u
                WHERE (:action IS NULL OR u.action = :action)
                  AND (:category IS NULL OR u.category = :category)
                  AND (:keyword IS NULL
                       OR LOWER(u.keyword) LIKE LOWER(CONCAT('%', :keyword, '%')))
                  AND (:startDate IS NULL OR u.createdAt >= :startDate)
                  AND (:endDate IS NULL OR u.createdAt < :endDate)
                  AND (:memberId IS NULL OR u.memberId = :memberId)
                  AND (
                      (:unclassified = true AND u.filterResult IS NULL)
                      OR
                      (:unclassified = false AND
                          (:filterResult IS NULL OR u.filterResult = :filterResult))
                  )
                  AND (:filterReason IS NULL OR u.filterReason = :filterReason)
                  AND (
                      :search IS NULL
                      OR LOWER(u.action) LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(CAST(u.category AS String))
                          LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(u.keyword) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                """,
            countQuery = """
                SELECT COUNT(u)
                FROM UserActionLog u
                WHERE (:action IS NULL OR u.action = :action)
                  AND (:category IS NULL OR u.category = :category)
                  AND (:keyword IS NULL
                       OR LOWER(u.keyword) LIKE LOWER(CONCAT('%', :keyword, '%')))
                  AND (:startDate IS NULL OR u.createdAt >= :startDate)
                  AND (:endDate IS NULL OR u.createdAt < :endDate)
                  AND (:memberId IS NULL OR u.memberId = :memberId)
                  AND ((:unclassified = true AND u.filterResult IS NULL) OR (:unclassified = false AND (:filterResult IS NULL OR u.filterResult = :filterResult)))
                  AND (:filterReason IS NULL OR u.filterReason = :filterReason)
                  AND (
                      :search IS NULL
                      OR LOWER(u.action) LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(CAST(u.category AS String))
                          LIKE LOWER(CONCAT('%', :search, '%'))
                      OR LOWER(u.keyword) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                """
    )
    Page<UserActionLog> searchAdminLogs(
            @Param("action") String action,
            @Param("category") RequestCategory category,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            @Param("memberId") Long memberId,
            @Param("filterResult") UserActionFilterResult filterResult,
            @Param("unclassified") boolean unclassified,
            @Param("filterReason") UserActionFilterReason filterReason,
            Pageable pageable
    );


    // ==========================================
    // 6. 관리자 카테고리별 행동 통계
    // INCLUDED 이벤트만 집계
    // ==========================================

    @Query("""
        SELECT u.category, COUNT(u)
        FROM UserActionLog u
        WHERE u.filterResult = :filterResult
        GROUP BY u.category
        ORDER BY COUNT(u) DESC
        """)
    List<Object[]> countLogsByCategory(
            @Param("filterResult") UserActionFilterResult filterResult
    );


    // ==========================================
    // 7. 광고 분석 및 캠페인 매칭용 행동 로그
    // INCLUDED 이벤트만 조회하도록 사용
    // ==========================================

    List<UserActionLog> findByFilterResult(
            UserActionFilterResult filterResult
    );


    // ==========================================
    // 8. 관리자 행동 유형별 통계
    // 기존 전체 로그 집계 유지
    // ==========================================

    @Query("""
        SELECT l.action, COUNT(l)
        FROM UserActionLog l
        GROUP BY l.action
        ORDER BY COUNT(l) DESC
        """)
    List<Object[]> countByActionType();


    // ==========================================
    // 9. 관리자 Kafka 행동 로그 필터링 통계
    // ==========================================

    // INCLUDED / EXCLUDED / NULL별 이벤트 수
    @Query("""
        SELECT u.filterResult, COUNT(u)
        FROM UserActionLog u
        GROUP BY u.filterResult
        """)
    List<Object[]> countByFilterResult();


    // 제외 사유별 이벤트 수
    @Query("""
        SELECT u.filterReason, COUNT(u)
        FROM UserActionLog u
        WHERE u.filterResult = :filterResult
        GROUP BY u.filterReason
        """)
    List<Object[]> countByFilterReason(
            @Param("filterResult") UserActionFilterResult filterResult
    );

    // 고객 행동 분석 - 행동 유형 및 카테고리별 집계
    @Query("""
    SELECT u.action, u.category, COUNT(u)
    FROM UserActionLog u
    WHERE u.filterResult = :result
      AND u.createdAt >= :start
      AND u.createdAt < :end
    GROUP BY u.action, u.category
    """)
    List<Object[]> behaviorGroups(
            @Param("result") UserActionFilterResult result,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 고객 행동 분석 - 활성 회원 수 집계
    @Query("""
    SELECT COUNT(DISTINCT u.memberId)
    FROM UserActionLog u
    WHERE u.filterResult = :result
      AND u.createdAt >= :start
      AND u.createdAt < :end
    """)
    long behaviorMembers(
            @Param("result") UserActionFilterResult result,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}