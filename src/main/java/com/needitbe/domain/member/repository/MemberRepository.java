package com.needitbe.domain.member.repository;

import com.needitbe.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    // 관리자 회원 검색 (아이디 또는 닉네임)
    Page<Member> findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
            String username,
            String nickname,
            Pageable pageable
    );
}