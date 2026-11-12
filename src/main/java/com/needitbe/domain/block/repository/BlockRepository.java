package com.needitbe.domain.block.repository;

import com.needitbe.domain.block.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerIdAndBlockedId(
            Long blockerId,
            Long blockedId
    );

    Optional<Block> findByBlockerIdAndBlockedId(
            Long blockerId,
            Long blockedId
    );

    List<Block> findByBlockerIdOrderByCreatedAtDesc(
            Long blockerId
    );

    @Query("""
    SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
    FROM Block b
    WHERE
        (b.blocker.id = :memberId1 AND b.blocked.id = :memberId2)
        OR
        (b.blocker.id = :memberId2 AND b.blocked.id = :memberId1)
""")
    boolean existsBlockBetweenMembers(
            @Param("memberId1") Long memberId1,
            @Param("memberId2") Long memberId2
    );
}