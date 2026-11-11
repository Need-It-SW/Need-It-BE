package com.needitbe.domain.search.repository;

import com.needitbe.domain.search.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository
        extends JpaRepository<SearchLog, Long> {

    // 최근 7일 인기 검색어 집계
    @Query("""
            SELECT s.keyword, COUNT(s)
            FROM SearchLog s
            WHERE s.createdAt >= :startDate
            GROUP BY s.keyword
            ORDER BY COUNT(s) DESC, s.keyword ASC
            """)
    List<Object[]> findPopularKeywords(
            @Param("startDate") LocalDateTime startDate
    );
}