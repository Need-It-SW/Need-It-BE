package com.needitbe.domain.block.service;

import com.needitbe.domain.block.dto.BlockResponse;
import com.needitbe.domain.block.entity.Block;
import com.needitbe.domain.block.repository.BlockRepository;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final MemberRepository memberRepository;

    // 1. 회원 차단
    @Transactional
    public BlockResponse blockMember(
            Long blockerId,
            Long blockedId
    ) {

        // 자기 자신 차단 방지
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException(
                    "자기 자신은 차단할 수 없습니다."
            );
        }

        // 차단한 회원 조회
        Member blocker = memberRepository.findById(blockerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원이 존재하지 않습니다."
                        )
                );

        // 차단 대상 회원 조회
        Member blocked = memberRepository.findById(blockedId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "차단 대상 회원이 존재하지 않습니다."
                        )
                );

        // 중복 차단 확인
        if (blockRepository.existsByBlockerIdAndBlockedId(
                blockerId,
                blockedId
        )) {
            throw new IllegalArgumentException(
                    "이미 차단한 회원입니다."
            );
        }

        Block block = new Block(blocker, blocked);

        blockRepository.save(block);

        return BlockResponse.from(block);
    }

    // 2. 회원 차단 해제
    @Transactional
    public void unblockMember(
            Long blockerId,
            Long blockedId
    ) {

        Block block = blockRepository
                .findByBlockerIdAndBlockedId(
                        blockerId,
                        blockedId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "차단하지 않은 회원입니다."
                        )
                );

        blockRepository.delete(block);
    }

    // 3. 내가 차단한 회원 목록
    @Transactional(readOnly = true)
    public List<BlockResponse> getMyBlocks(Long memberId) {

        return blockRepository
                .findByBlockerIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(BlockResponse::from)
                .toList();
    }
}