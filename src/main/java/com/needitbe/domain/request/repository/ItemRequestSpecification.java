package com.needitbe.domain.request.repository;

import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.request.entity.RequestStatus;
import org.springframework.data.jpa.domain.Specification;

public class ItemRequestSpecification {

    // 지역 필터
    public static Specification<ItemRequest> hasRegion(String region) {

        return (root, query, cb) -> {

            if (region == null || region.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(root.get("region"), region);
        };
    }

    // 카테고리 필터
    public static Specification<ItemRequest> hasCategory(
            RequestCategory category
    ) {

        return (root, query, cb) -> {

            if (category == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("category"), category);
        };
    }

    // 상태 필터
    public static Specification<ItemRequest> hasStatus(
            RequestStatus status
    ) {

        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), status);
        };
    }

    // =====================================================
    // 키워드 검색 (제목 또는 내용)
    // =====================================================
    public static Specification<ItemRequest> hasKeyword(String keyword) {

        return (root, query, cb) -> {

            // 검색어가 없으면 전체 조회
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            // 검색어 앞뒤 공백 제거
            String searchKeyword = keyword.trim();

            // LIKE 특수문자 이스케이프
            String escapedKeyword = searchKeyword
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");

            String pattern = "%" + escapedKeyword.toLowerCase() + "%";

            // 제목 검색
            var titlePredicate = cb.like(
                    cb.lower(root.get("title")),
                    pattern,
                    '\\'
            );

            // 내용 검색
            var descriptionPredicate = cb.like(
                    cb.lower(root.get("description")),
                    pattern,
                    '\\'
            );

            // 제목 또는 내용에 검색어가 포함되면 조회
            return cb.or(
                    titlePredicate,
                    descriptionPredicate
            );
        };
    }
}