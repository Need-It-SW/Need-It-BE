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

import com.needitbe.domain.useraction.kafka.UserActionProducer;

import com.needitbe.domain.request.entity.RequestCategory;

import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.support.TransactionSynchronization;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import java.util.UUID;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final ItemRequestRepository itemRequestRepository;

    private final UserActionProducer userActionProducer;
    private final ObjectMapper objectMapper;

    // =====================================================
// 1. 관심 게시글 등록
// =====================================================
    @Transactional
    public void addFavorite(Long memberId, Long itemRequestId) {

        // 1. 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 회원입니다."
                        )
                );

        // 2. 게시글 존재 여부 확인
        ItemRequest itemRequest =
                itemRequestRepository.findById(itemRequestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 요청 게시글입니다."
                                )
                        );

        // 3. 중복 관심 등록 방지
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

        // 4. 관심 게시글 저장
        Favorite favorite = new Favorite(
                member,
                itemRequest
        );

        favoriteRepository.save(favorite);

        // 5. 실제 게시글 카테고리 가져오기
        RequestCategory category = itemRequest.getCategory();

        // 6. Kafka FAVORITE 이벤트 발행 예약
        publishFavoriteEvent(
                memberId,
                itemRequestId,
                category
        );
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

    // =====================================================
// Kafka FAVORITE 이벤트 발행
// =====================================================
    private void publishFavoriteEvent(
            Long memberId,
            Long requestId,
            RequestCategory category
    ) {

        try {

            // 1. 이벤트 고유 ID 생성
            String eventId = UUID.randomUUID().toString();

            // 2. Kafka 메시지 생성
            Map<String, Object> event = Map.of(
                    "eventId", eventId,
                    "userId", memberId,
                    "action", "FAVORITE",
                    "requestId", requestId,
                    "category", category.name()
            );

            // 3. JSON 변환
            String message = objectMapper.writeValueAsString(event);

            // 4. DB 커밋 이후 Kafka 전송
            if (TransactionSynchronizationManager.isActualTransactionActive()
                    && TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                sendFavoriteEvent(
                                        memberId,
                                        requestId,
                                        message
                                );
                            }
                        }
                );

            } else {

                sendFavoriteEvent(
                        memberId,
                        requestId,
                        message
                );
            }

        } catch (Exception e) {

            log.error(
                    "FAVORITE 이벤트 생성 실패 - memberId: {}, requestId: {}",
                    memberId,
                    requestId,
                    e
            );
        }
    }

    private void sendFavoriteEvent(
            Long memberId,
            Long requestId,
            String message
    ) {

        try {

            userActionProducer.send(
                    memberId.toString(),
                    message
            );

            log.info(
                    "FAVORITE 이벤트 발행 요청 - memberId: {}, requestId: {}",
                    memberId,
                    requestId
            );

        } catch (Exception e) {

            log.error(
                    "FAVORITE 이벤트 발행 실패 - memberId: {}, requestId: {}",
                    memberId,
                    requestId,
                    e
            );
        }
    }
}