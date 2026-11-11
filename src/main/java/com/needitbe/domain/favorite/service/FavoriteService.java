package com.needitbe.domain.favorite.service;

import com.needitbe.domain.favorite.entity.Favorite;
import com.needitbe.domain.favorite.repository.FavoriteRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.repository.ItemRequestRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.needitbe.domain.favorite.dto.FavoriteResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final ItemRequestRepository itemRequestRepository;

    // =====================================================
    // 1. 관심 게시글 등록
    // =====================================================
    @Transactional
    public void addFavorite(Long memberId, Long itemRequestId) {

        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                        )
                );

        // 요청 게시글 존재 여부 확인
        ItemRequest itemRequest =
                itemRequestRepository.findById(itemRequestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 요청 게시글입니다."
                                )
                        );

        // 중복 관심 등록 방지
        boolean alreadyFavorite =
                favoriteRepository.existsByMemberIdAndItemRequestId(
                        memberId,
                        itemRequestId
                );

        if (alreadyFavorite) {
            throw new IllegalArgumentException(
                    "이미 관심 등록한 게시글입니다."
            );
        }

        // 관심 게시글 저장
        Favorite favorite = new Favorite(
                member,
                itemRequest
        );

        favoriteRepository.save(favorite);
    }

    // =====================================================
    // 2. 관심 게시글 해제
    // =====================================================
    @Transactional
    public void removeFavorite(Long memberId, Long itemRequestId) {

        // 본인이 등록한 관심 게시글 조회
        Favorite favorite =
                favoriteRepository.findByMemberIdAndItemRequestId(
                        memberId,
                        itemRequestId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "관심 등록되지 않은 게시글입니다."
                        )
                );

        // 관심 등록 해제
        favoriteRepository.delete(favorite);
    }

    // =====================================================
    // 3. 내 관심 게시글 목록 조회
    // =====================================================
    public List<FavoriteResponse> getMyFavorites(Long memberId) {

        return favoriteRepository.findByMemberIdOrderByCreatedAtDesc(
                        memberId
                )
                .stream()
                .map(FavoriteResponse::from)
                .toList();
    }
}