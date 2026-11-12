package com.needitbe.domain.offer.service;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;

import com.needitbe.domain.notification.entity.NotificationType;
import com.needitbe.domain.notification.service.NotificationService;
import com.needitbe.domain.offer.dto.OfferCreateRequest;
import com.needitbe.domain.offer.dto.OfferImageResponse;
import com.needitbe.domain.offer.dto.OfferResponse;
import com.needitbe.domain.offer.dto.OfferSentResponse;
import com.needitbe.domain.offer.entity.Offer;
import com.needitbe.domain.offer.entity.OfferImage;
import com.needitbe.domain.offer.repository.OfferRepository;

import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.entity.RequestStatus;
import com.needitbe.domain.request.repository.ItemRequestRepository;

import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;

import com.needitbe.global.s3.S3Service;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import com.needitbe.domain.offer.repository.OfferImageRepository;
import com.needitbe.domain.offer.dto.OfferReceivedResponse;

import com.needitbe.domain.offer.entity.OfferStatus;
import com.needitbe.domain.offer.dto.OfferAcceptanceCancelRequest;
import com.needitbe.domain.offer.entity.OfferAcceptanceCancellation;
import com.needitbe.domain.offer.repository.OfferAcceptanceCancellationRepository;

import com.needitbe.domain.chat.entity.ChatRoom;
import com.needitbe.domain.chat.repository.ChatRoomRepository;

import com.needitbe.domain.block.repository.BlockRepository;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final BlockRepository blockRepository;
    private final OfferAcceptanceCancellationRepository cancellationRepository;
    private final OfferRepository offerRepository;
    private final OfferImageRepository offerImageRepository;
    private final ItemRequestRepository itemRequestRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationService notificationService;

    // =====================================================
    // 1. 물품 제공 제안 등록
    // =====================================================
    // =====================================================
// 1. 물품 제공 제안 등록 + 알림 생성
// =====================================================
    @Transactional
    public OfferResponse createOffer(
            Long memberId,
            Long requestId,
            OfferCreateRequest request
    ) {

        // 1. 요청 게시글 조회
        ItemRequest itemRequest =
                itemRequestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                        );


        // 2. 자기 게시글인지 확인
        if (itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.OFFER_SELF_NOT_ALLOWED);
        }

// ==========================================
// 3. ⭐ 회원 차단 관계 확인
// ==========================================

// 요청 게시글 작성자 ID
        Long requestOwnerId = itemRequest.getMember().getId();

// 양방향 차단 관계 확인
        boolean isBlocked = blockRepository.existsBlockBetweenMembers(
                memberId,
                requestOwnerId
        );

        if (isBlocked) {
            throw new CustomException(ErrorCode.OFFER_BLOCKED);
        }

// 4. 마감된 게시글인지 확인
        if (itemRequest.getStatus() == RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.OFFER_REQUEST_CLOSED);
        }

        // 4. 제안 작성자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // 5. Offer 엔티티 생성
        Offer offer = new Offer(
                itemRequest,
                member,
                request.itemDescription(),
                request.message()
        );

        // 6. 제안 DB 저장
        Offer savedOffer = offerRepository.saveAndFlush(offer);

        // ==========================================
        // 7. 요청글 작성자에게 알림 생성
        // ==========================================

        Long receiverId = itemRequest.getMember().getId();

        notificationService.createNotification(
                receiverId,
                NotificationType.OFFER_RECEIVED,
                "새로운 나눔 제안이 도착했어요.",
                savedOffer.getId()
        );

        // 8. 응답 반환
        return OfferResponse.from(savedOffer);
    }

    // =====================================================
// 2. 나눔 제안 이미지 업로드
// =====================================================
    @Transactional
    public List<OfferImageResponse> uploadOfferImages(
            Long memberId,
            Long offerId,
            List<MultipartFile> files
    ) {

        // 1. 제안 조회
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("제안을 찾을 수 없습니다.")
                );

        // 2. 작성자 확인
        if (!offer.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 3. 이미지 개수 확인
        int currentCount = offer.getImages().size();

        if (files == null || files.isEmpty()
                || currentCount + files.size() > 3) {
            throw new IllegalArgumentException(
                    "제안 이미지는 최대 3장까지 등록할 수 있습니다."
            );
        }

        // 4. S3에 업로드한 이미지 키 저장
        List<String> uploadedKeys = new ArrayList<>();

        try {

            for (int i = 0; i < files.size(); i++) {

                // S3 이미지 업로드
                String imageKey = s3Service.uploadOfferImage(
                        files.get(i),
                        offerId
                );

                uploadedKeys.add(imageKey);

                // 이미지 URL 생성
                String imageUrl = s3Service.getImageUrl(imageKey);

                // Offer 엔티티에 이미지 추가
                offer.addImage(
                        imageUrl,
                        imageKey,
                        currentCount + i
                );
            }

            // 5. DB 저장
            offerRepository.saveAndFlush(offer);

            // 6. Presigned URL 응답
            return offer.getImages()
                    .stream()
                    .map(image ->
                            OfferImageResponse.from(
                                    image,
                                    s3Service.getPresignedUrl(
                                            image.getImageKey()
                                    )
                            )
                    )
                    .toList();

        } catch (Exception e) {

            // 업로드 도중 실패하면 S3 이미지 정리
            for (String key : uploadedKeys) {
                try {
                    s3Service.deleteImage(key);
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }

            throw e;
        }
    }
    @Transactional
    public void deleteOfferImage(
            Long memberId,
            Long offerId,
            Long imageId
    ) {

        // 1. 제안 조회
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("제안을 찾을 수 없습니다.")
                );

        // 2. 작성자 확인
        if (!offer.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 3. 삭제할 이미지 조회
        OfferImage image = offerImageRepository
                .findByIdAndOfferId(imageId, offerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("이미지를 찾을 수 없습니다.")
                );

        // 4. S3 이미지 삭제
        s3Service.deleteImage(image.getImageKey());

        // 5. DB 이미지 삭제
        offer.removeImage(image);
    }

    // =====================================================
// 3. 내가 보낸 제안 목록 조회
// =====================================================
    @Transactional(readOnly = true)
    public List<OfferSentResponse> getSentOffers(Long memberId) {

        // 1. 내가 작성한 제안 조회
        List<Offer> offers =
                offerRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        // 2. 응답 DTO 변환
        return offers.stream()
                .map(offer -> {

                    // 제안 이미지 목록
                    List<OfferImageResponse> images =
                            offer.getImages()
                                    .stream()
                                    .map(image ->
                                            OfferImageResponse.from(
                                                    image,
                                                    s3Service.getPresignedUrl(
                                                            image.getImageKey()
                                                    )
                                            )
                                    )
                                    .toList();

                    return OfferSentResponse.from(offer, images);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferReceivedResponse> getReceivedOffers(Long memberId) {

        List<Offer> offers =
                offerRepository.findByItemRequestMemberIdOrderByCreatedAtDesc(memberId);

        return offers.stream()
                .map(offer -> {

                    List<OfferImageResponse> images =
                            offer.getImages()
                                    .stream()
                                    .map(image ->
                                            OfferImageResponse.from(
                                                    image,
                                                    s3Service.getPresignedUrl(
                                                            image.getImageKey()
                                                    )
                                            )
                                    )
                                    .toList();

                    return OfferReceivedResponse.from(offer, images);
                })
                .toList();
    }

    // =====================================================
// 4. 나눔 제안 수락 + 채팅방 자동 생성
// =====================================================
    @Transactional
    public OfferResponse acceptOffer(
            Long memberId,
            Long offerId
    ) {

        // 1. 요청 게시글 잠금 후 제안 조회
        Offer offer = getOfferWithRequestLock(offerId);

        // 2. 요청 게시글 작성자 확인
        if (!offer.getItemRequest().getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.OFFER_ACCESS_DENIED);
        }
        // ==========================================
// ⭐ 차단 관계 확인
// ==========================================

        Long offerMemberId = offer.getMember().getId();

        if (blockRepository.existsBlockBetweenMembers(
                memberId,
                offerMemberId
        )) {
            throw new CustomException(ErrorCode.OFFER_BLOCKED_ACCEPT);
        }

        // 3. 마감된 게시글인지 확인
        if (offer.getItemRequest().getStatus() == RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.OFFER_REQUEST_CLOSED);
        }

        // 4. 제안 상태 확인
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_PROCESSED);
        }

        // 5. 이미 수락된 제안이 있는지 확인
        boolean alreadyAccepted =
                offerRepository.existsByItemRequestIdAndStatus(
                        offer.getItemRequest().getId(),
                        OfferStatus.ACCEPTED
                );

        if (alreadyAccepted) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_ACCEPTED);
        }

        // 6. 제안 수락
        offer.accept();

        // 7. 제안 상태 저장
        offerRepository.saveAndFlush(offer);

        // 8. 채팅방 중복 생성 방지
        if (!chatRoomRepository.existsByOfferId(offerId)) {

            // 9. 채팅방 생성
            ChatRoom chatRoom = new ChatRoom(offer);

            // 10. 채팅방 저장
            chatRoomRepository.saveAndFlush(chatRoom);
        }

        // 11. 응답 반환
        // ==========================================
// 11. 제안 작성자에게 수락 알림 생성
// ==========================================

        Long receiverId = offer.getMember().getId();

        notificationService.createNotification(
                receiverId,
                NotificationType.OFFER_ACCEPTED,
                "보낸 나눔 제안이 수락되었어요.",
                offer.getId()
        );

// 12. 응답 반환
        return OfferResponse.from(offer);
    }


    // =====================================================
// 5. 나눔 제안 거절
// =====================================================
    @Transactional
    public OfferResponse rejectOffer(
            Long memberId,
            Long offerId
    ) {

        Offer offer = getOfferWithRequestLock(offerId);

        // 요청 게시글 작성자 확인
        if (!offer.getItemRequest().getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.OFFER_ACCESS_DENIED);
        }

        if (offer.getItemRequest().getStatus() == RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.OFFER_REQUEST_CLOSED);
        }

        // 이미 처리된 제안인지 확인
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_PROCESSED);
        }

        // 다른 제안이 수락되어 있으면 거절 불가능
        boolean alreadyAccepted =
                offerRepository.existsByItemRequestIdAndStatus(
                        offer.getItemRequest().getId(),
                        OfferStatus.ACCEPTED
                );

        if (alreadyAccepted) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_ACCEPTED);
        }

        offer.reject();

        offerRepository.saveAndFlush(offer);

        return OfferResponse.from(offer);
    }


    // =====================================================
// 6. 나눔 제안 취소
// =====================================================
    @Transactional
    public OfferResponse cancelOffer(
            Long memberId,
            Long offerId
    ) {

        // 1. 요청 게시글 잠금 후 제안 조회
        Offer offer = getOfferWithRequestLock(offerId);

        // 2. 제안 작성자 확인
        if (!offer.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.OFFER_ACCESS_DENIED);
        }

        // 3. 제안 상태 확인
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_PROCESSED);
        }

        // 4. 제안 취소
        offer.cancel();

        // 5. DB 저장
        offerRepository.saveAndFlush(offer);

        return OfferResponse.from(offer);
    }



    private Offer getOfferWithRequestLock(Long offerId) {

        // 제안이 연결된 요청 게시글 ID 조회
        Long requestId = offerRepository.findRequestIdByOfferId(offerId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.OFFER_NOT_FOUND)
                );

        // 요청 게시글 잠금
        itemRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 잠금 획득 후 제안 조회
        return offerRepository.findById(offerId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.OFFER_NOT_FOUND)
                );
    }

    // =====================================================
// 7. 나눔 제안 수락 취소 + 취소 사유 저장
// =====================================================
    @Transactional
    public OfferResponse cancelOfferAcceptance(
            Long memberId,
            Long offerId,
            OfferAcceptanceCancelRequest request
    ) {

        // 1. 요청 게시글 잠금 후 제안 조회
        Offer offer = getOfferWithRequestLock(offerId);

        // 2. 요청 게시글 작성자 확인
        if (!offer.getItemRequest().getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.OFFER_ACCESS_DENIED);
        }

        // 3. ACCEPTED 상태인지 확인
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_PROCESSED);
        }

        // 4. 취소 사유 확인
        String reason = request.reason().trim();

        // 5. 취소 실행 (ACCEPTED → REJECTED)
        offer.cancelAcceptance();

        // 6. 취소한 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // 7. 취소 기록 생성
        OfferAcceptanceCancellation cancellation =
                new OfferAcceptanceCancellation(
                        offer,
                        member,
                        reason
                );

        // 8. 제안 상태 저장
        offerRepository.saveAndFlush(offer);

        // 9. 취소 기록 저장
        cancellationRepository.save(cancellation);

        return OfferResponse.from(offer);
    }

    // =====================================================
// 8. 나눔 거래 완료
// =====================================================
    @Transactional
    public OfferResponse completeOffer(
            Long memberId,
            Long offerId
    ) {

        // 1. 요청 게시글 잠금 후 제안 조회
        Offer offer = getOfferWithRequestLock(offerId);

        // 2. 요청 게시글 조회
        ItemRequest itemRequest = offer.getItemRequest();

        // 3. 요청 게시글 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.OFFER_ACCESS_DENIED);
        }

        // 4. 이미 완료된 거래인지 확인
        if (offer.getStatus() == OfferStatus.COMPLETED) {
            throw new CustomException(ErrorCode.OFFER_ALREADY_COMPLETED);
        }

        // 5. 수락된 제안인지 확인
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new CustomException(ErrorCode.OFFER_NOT_ACCEPTED);
        }

        // 6. 요청 게시글이 이미 마감됐는지 확인
        if (itemRequest.getStatus() == RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.OFFER_REQUEST_CLOSED);
        }

        // 7. 제안 거래 완료 처리
        offer.complete();

        // 8. 요청 게시글 마감
        itemRequest.close();

        // 9. DB 저장
        offerRepository.saveAndFlush(offer);
        itemRequestRepository.saveAndFlush(itemRequest);

        return OfferResponse.from(offer);
    }


}