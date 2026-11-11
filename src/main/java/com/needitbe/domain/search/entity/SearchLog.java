package com.needitbe.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "search_logs",
        indexes = {
                @Index(
                        name = "idx_search_keyword_created",
                        columnList = "keyword, created_at"
                ),
                @Index(
                        name = "idx_search_created_at",
                        columnList = "created_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 검색 키워드
    @Column(nullable = false, length = 100)
    private String keyword;

    // 검색한 시간
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    // 검색 기록 생성
    public SearchLog(String keyword) {
        this.keyword = keyword;
    }

    // 최초 저장 시 검색 시간 기록
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
