package com.needitbe.domain.useraction.repository;

import com.needitbe.domain.useraction.entity.UserActionLog;
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

    // 회원별 최근 행동 로그 100개 조회
    List<UserActionLog> findTop100ByMemberIdOrderByCreatedAtDescIdDesc(
            Long memberId
    );

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
            Pageable pageable
    );

    @Query("""
    SELECT u.category, COUNT(u)
    FROM UserActionLog u
    GROUP BY u.category
    ORDER BY COUNT(u) DESC
    """)
    List<Object[]> countLogsByCategory();

    // 대시보드 캠페인 조건 매칭용 행동 로그 조회
    @Query("""
    SELECT u
    FROM UserActionLog u
    WHERE u.category IS NOT NULL
       OR u.keyword IS NOT NULL
       OR u.action IS NOT NULL
    """)
    List<UserActionLog> findLogsForDashboardMatching();

    @Query("""
    SELECT l.action, COUNT(l)
    FROM UserActionLog l
    GROUP BY l.action
    ORDER BY COUNT(l) DESC
""")
    List<Object[]> countByActionType();
}