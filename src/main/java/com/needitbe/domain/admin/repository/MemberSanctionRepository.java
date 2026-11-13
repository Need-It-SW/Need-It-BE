package com.needitbe.domain.admin.repository;

import com.needitbe.domain.admin.entity.MemberSanction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberSanctionRepository
        extends JpaRepository<MemberSanction, Long> {

    // 회원의 전체 제재 이력
    List<MemberSanction> findByMemberIdOrderByStartedAtDesc(Long memberId);

    // 해제되지 않은 가장 최근 제재
    Optional<MemberSanction>
    findFirstByMemberIdAndReleasedAtIsNullOrderByStartedAtDesc(Long memberId);

    // 기간이 만료됐지만 아직 해제되지 않은 제재 조회
    List<MemberSanction> findByReleasedAtIsNullAndExpiresAtLessThanEqual(
            LocalDateTime now
    );
}