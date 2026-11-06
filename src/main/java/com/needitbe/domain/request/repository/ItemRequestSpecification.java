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
}