package com.needitbe.domain.request.service;

import com.needitbe.domain.favorite.repository.FavoriteRepository;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;
import com.needitbe.domain.offer.entity.OfferStatus;
import com.needitbe.domain.offer.repository.OfferRepository;
import com.needitbe.domain.request.dto.RequestCreateRequest;
import com.needitbe.domain.request.dto.RequestImageResponse;
import com.needitbe.domain.request.dto.RequestResponse;
import com.needitbe.domain.request.dto.RequestUpdateRequest;
import com.needitbe.domain.request.entity.ItemRequest;
import com.needitbe.domain.request.entity.ItemRequestImage;
import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.request.entity.RequestStatus;
import com.needitbe.domain.request.repository.ItemRequestImageRepository;
import com.needitbe.domain.request.repository.ItemRequestRepository;
import com.needitbe.domain.request.repository.ItemRequestSpecification;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.global.s3.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.needitbe.domain.useraction.kafka.UserActionProducer;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;
import com.needitbe.domain.offer.entity.OfferStatus;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {


    private final SimpMessagingTemplate messagingTemplate;
    private final UserActionProducer userActionProducer;
    private final ObjectMapper objectMapper;
    private final FavoriteRepository favoriteRepository;
    private final OfferRepository offerRepository;
    private final ItemRequestRepository itemRequestRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;
    private final ItemRequestImageRepository itemRequestImageRepository;

    // =====================================================
    // 1. 요청 게시글 작성
    // =====================================================
    @Transactional
    public RequestResponse createRequest(
            Long memberId,
            RequestCreateRequest request
    ) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        ItemRequest itemRequest = new ItemRequest(
                member,
                request.title(),
                request.description(),
                request.category(),
                request.region()
        );

        ItemRequest savedRequest =
                itemRequestRepository.save(itemRequest);

// 요청 등록 행동 이벤트 발행 예약
        publishRequestCreateEvent(
                memberId,
                savedRequest.getId(),
                savedRequest.getCategory()
        );

        // Publish only after the new request is visible to other database readers.
        Long newRequestId = savedRequest.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            Map<String, Object> payload = Map.of(
                                    "type", "REQUEST_CREATED",
                                    "requestId", newRequestId
                            );

                            messagingTemplate.convertAndSend(
                                    "/topic/requests",
                                    (Object) payload
                            );
                        } catch (Exception ex) {
                            log.warn("요청글 실시간 알림 전송 실패: {}", newRequestId, ex);
                        }
                    }
                }
        );

        return toResponse(savedRequest);
    }

    // =====================================================
    // 2. 요청 게시글 목록 조회 (비로그인)
    // =====================================================
    @Transactional(readOnly = true)
    public Page<RequestResponse> getRequests(
            String region,
            RequestCategory category,
            RequestStatus status,
            String keyword,
            String sort,
            int page,
            int size
    ) {

        // 정렬 조건 생성
        Sort sorting = createSort(sort);

        // 페이지네이션 + 정렬
        Pageable pageable = PageRequest.of(
                page,
                size,
                sorting
        );

        // 필터 조건
        Specification<ItemRequest> specification =
                Specification.allOf(
                        ItemRequestSpecification.isNotDeleted(),
                        ItemRequestSpecification.hasRegion(region),
                        ItemRequestSpecification.hasCategory(category),
                        ItemRequestSpecification.hasStatus(status),
                        ItemRequestSpecification.hasKeyword(keyword)
                );

        // 게시글 목록 조회
        Page<ItemRequest> requests =
                itemRequestRepository.findAll(specification, pageable);

        // 비로그인 사용자는 isFavorite = false
        return mapRequestsWithTradeStatus(requests, null);
    }

    // =====================================================
    // 3. 요청 게시글 목록 조회 (로그인)
    // =====================================================
    @Transactional(readOnly = true)
    public Page<RequestResponse> getRequests(
            Long memberId,
            String region,
            RequestCategory category,
            RequestStatus status,
            String keyword,
            String sort,
            int page,
            int size
    ) {

        // 정렬 조건 생성
        Sort sorting = createSort(sort);

        // 페이지네이션 + 정렬
        Pageable pageable = PageRequest.of(
                page,
                size,
                sorting
        );

        // 필터 조건
        Specification<ItemRequest> specification =
                Specification.allOf(
                        ItemRequestSpecification.isNotDeleted(),
                        ItemRequestSpecification.hasRegion(region),
                        ItemRequestSpecification.hasCategory(category),
                        ItemRequestSpecification.hasStatus(status),
                        ItemRequestSpecification.hasKeyword(keyword)
                );

        // 게시글 목록 조회
        Page<ItemRequest> requests =
                itemRequestRepository.findAll(specification, pageable);

        // 현재 페이지의 게시글 ID 수집
        List<Long> requestIds = requests.getContent()
                .stream()
                .map(ItemRequest::getId)
                .toList();

        // 로그인 회원이 관심 등록한 게시글 ID 조회
        Set<Long> favoriteIds = requestIds.isEmpty()
                ? Set.of()
                : new HashSet<>(
                favoriteRepository.findFavoriteRequestIds(
                        memberId,
                        requestIds
                )
        );

        // 관심 여부를 포함한 DTO 반환
        return mapRequestsWithTradeStatus(requests, favoriteIds);
    }

    // =====================================================
    // 4. 요청 게시글 상세 조회 (비로그인)
    // =====================================================
    @Transactional
    public RequestResponse getRequest(Long requestId) {
        itemRequestRepository.incrementViewCount(requestId);


        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        return toResponse(itemRequest);
    }

    // =====================================================

// 5. 요청 게시글 상세 조회 (로그인)

// =====================================================

    @Transactional

    public RequestResponse getRequest(

            Long memberId,

            Long requestId

    ) {
        itemRequestRepository.incrementViewCount(requestId);


        // 1. 게시글 조회

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)

                .orElseThrow(() ->

                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)

                );

        // 2. 관심 등록 여부 확인

        boolean isFavorite =

                favoriteRepository.existsByMemberIdAndItemRequestId(

                        memberId,

                        requestId

                );

        // 3. 기존 응답 DTO 생성

        RequestResponse response = toResponse(itemRequest, isFavorite);

        // 4. 게시글 카테고리 확보

        RequestCategory category = itemRequest.getCategory();

        // 5. Kafka VIEW 이벤트 발행 예약

        publishViewEvent(memberId, requestId, category);

        return response;

    }
    // =====================================================
    // 6. 요청 게시글 수정
    // =====================================================
    @Transactional
    public RequestResponse updateRequest(
            Long memberId,
            Long requestId,
            RequestUpdateRequest request
    ) {

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        itemRequest.update(
                request.title(),
                request.description(),
                request.category(),
                request.region()
        );

        return toResponse(itemRequest);
    }

    // =====================================================
// 7. 요청 게시글 삭제
// =====================================================
    @Transactional
    public void deleteRequest(
            Long memberId,
            Long requestId
    ) {

        // 1. 게시글 조회 및 잠금
        ItemRequest itemRequest =
                itemRequestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                        );

        // 2. 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 3. 이미 삭제된 게시글인지 확인
        if (itemRequest.isDeleted()) {
            throw new CustomException(ErrorCode.REQUEST_NOT_FOUND);
        }

        // 진행 중인 요청에 제안이 있으면 먼저 요청을 마감하도록 안내한다.
        // 마감된 요청은 제안/채팅/후기를 보존하면서 목록에서 숨긴다.
        boolean hasOffers = offerRepository.existsByItemRequestId(requestId);
        if (hasOffers && itemRequest.getStatus() != RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.REQUEST_HAS_OFFERS);
        }

        itemRequest.softDelete();
        log.info("요청 게시글 소프트 삭제 - requestId: {}", requestId);
    }

    // =====================================================
    // 8. 요청 게시글 마감
    // =====================================================
    @Transactional
    public RequestResponse closeRequest(
            Long memberId,
            Long requestId
    ) {

        ItemRequest itemRequest =
                itemRequestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                        );

        // 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 이미 마감된 게시글인지 확인
        if (itemRequest.getStatus() == RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.REQUEST_ALREADY_CLOSED);
        }

        // 수락된 제안이 있는지 확인
        boolean hasAcceptedOffer =
                offerRepository.existsByItemRequestIdAndStatus(
                        requestId,
                        OfferStatus.ACCEPTED
                );

        // 수락된 제안이 있다면 직접 마감 불가능
        if (hasAcceptedOffer) {
            throw new CustomException(
                    ErrorCode.REQUEST_HAS_ACCEPTED_OFFER
            );
        }

        // 요청 게시글 마감
        itemRequest.close();

        return toResponse(itemRequest);
    }

    // =====================================================
    // 9. 내가 작성한 요청 게시글 조회
    // =====================================================
    @Transactional(readOnly = true)
    public Page<RequestResponse> getMyRequests(
            Long memberId,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<ItemRequest> requests =
                itemRequestRepository.findByMemberIdAndDeletedFalse(
                        memberId,
                        pageable
                );

        return requests.map(this::toResponse);
    }

    // =====================================================
    // 10. 요청 게시글 이미지 업로드
    // =====================================================
    @Transactional
    public List<RequestImageResponse> uploadImages(
            Long memberId,
            Long requestId,
            List<MultipartFile> files
    ) {

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 이미지 개수 확인
        int currentCount = itemRequest.getImages().size();

        if (files == null || files.isEmpty()
                || currentCount + files.size() > 5) {

            throw new IllegalArgumentException(
                    "이미지는 게시글당 최대 5장까지 등록할 수 있습니다."
            );
        }

        // S3 업로드
        List<String> uploadedKeys = new ArrayList<>();

        try {

            for (int i = 0; i < files.size(); i++) {

                String imageKey = s3Service.uploadImage(
                        files.get(i),
                        requestId
                );

                uploadedKeys.add(imageKey);

                String imageUrl =
                        s3Service.getImageUrl(imageKey);

                itemRequest.addImage(
                        imageUrl,
                        imageKey,
                        currentCount + i
                );
            }

            // DB 저장
            itemRequestRepository.saveAndFlush(itemRequest);

            // 이미지 응답 반환
            return itemRequest.getImages()
                    .stream()
                    .map(image -> RequestImageResponse.from(
                            image,
                            s3Service.getPresignedUrl(
                                    image.getImageKey()
                            )
                    ))
                    .toList();

        } catch (Exception e) {

            // 업로드 실패 시 S3 파일 정리
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

    // =====================================================
    // 11. 요청 게시글 개별 이미지 삭제
    // =====================================================
    @Transactional
    public void deleteImage(
            Long memberId,
            Long requestId,
            Long imageId
    ) {

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        ItemRequestImage image =
                itemRequestImageRepository
                        .findByIdAndItemRequestId(imageId, requestId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.REQUEST_IMAGE_NOT_FOUND
                                )
                        );

        // S3 이미지 키 보관
        String imageKey = image.getImageKey();

        // DB 이미지 제거
        itemRequest.removeImage(image);

        // DB 커밋 후 S3 이미지 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {

                            s3Service.deleteImage(imageKey);

                        } catch (Exception e) {

                            log.error(
                                    "S3 이미지 삭제 실패. imageKey={}",
                                    imageKey,
                                    e
                            );
                        }
                    }
                }
        );
    }

    // =====================================================
    // 12. 게시글 응답 DTO 변환 (기본)
    // =====================================================
    private RequestResponse toResponse(ItemRequest request) {

        return toResponse(request, false);
    }

    // =====================================================
    // 13. 게시글 응답 DTO 변환 (관심 여부 포함)
    // =====================================================
    private RequestResponse toResponse(
            ItemRequest request,
            boolean isFavorite
    ) {

        List<RequestImageResponse> images = request.getImages()
                .stream()
                .map(image -> RequestImageResponse.from(
                        image,
                        s3Service.getPresignedUrl(
                                image.getImageKey()
                        )
                ))
                .toList();

        return RequestResponse.from(
                request,
                images,
                isFavorite,
                resolveTradeStatus(request.getId(), request.getStatus())
        );
    }

    // 목록에서 제안 상태를 한 번에 조회하여 N+1 쿼리를 방지합니다.
    private Page<RequestResponse> mapRequestsWithTradeStatus(Page<ItemRequest> page, Set<Long> favoriteIds) {
        List<Long> ids = page.getContent().stream().map(ItemRequest::getId).toList();
        Map<Long, String> states = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : offerRepository.findStatusesByRequestIds(
                    ids, List.of(OfferStatus.COMPLETED, OfferStatus.ACCEPTED))) {
                Long id = (Long) row[0];
                OfferStatus status = (OfferStatus) row[1];
                if (status == OfferStatus.COMPLETED) states.put(id, "COMPLETED");
                else states.putIfAbsent(id, "IN_PROGRESS");
            }
        }
        return page.map(request -> {
            String tradeStatus = states.getOrDefault(request.getId(),
                    request.getStatus() == RequestStatus.CLOSED ? "CLOSED" : "OPEN");
            boolean isFavorite = favoriteIds != null && favoriteIds.contains(request.getId());
            return toResponseWithTradeStatus(request, isFavorite, tradeStatus);
        });
    }

    private String resolveTradeStatus(Long requestId, RequestStatus status) {
        if (offerRepository.existsByItemRequestIdAndStatus(requestId, OfferStatus.COMPLETED)) return "COMPLETED";
        if (offerRepository.existsByItemRequestIdAndStatus(requestId, OfferStatus.ACCEPTED)) return "IN_PROGRESS";
        return status == RequestStatus.CLOSED ? "CLOSED" : "OPEN";
    }

    private RequestResponse toResponseWithTradeStatus(ItemRequest request, boolean isFavorite, String tradeStatus) {
        List<RequestImageResponse> images = request.getImages().stream()
                .map(image -> RequestImageResponse.from(image, s3Service.getPresignedUrl(image.getImageKey())))
                .toList();
        return RequestResponse.from(request, images, isFavorite, tradeStatus);
    }

    // =====================================================
    // 14. 게시글 정렬 조건 생성
    // =====================================================
    private Sort createSort(String sort) {

        // 오래된순
        if ("oldest".equalsIgnoreCase(sort)) {

            return Sort.by(Sort.Direction.ASC, "createdAt")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
        }

        // 최신순 (기본값)
        return Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }

    // =====================================================
// Kafka VIEW 이벤트 발행
// =====================================================
    private void publishViewEvent(
            Long memberId,
            Long requestId,
            RequestCategory category
    ) {

        try {

            // 1. Kafka 이벤트 생성
            String eventId = UUID.randomUUID().toString();

            Map<String, Object> event = Map.of(
                    "eventId", eventId,
                    "userId", memberId,
                    "action", "VIEW",
                    "requestId", requestId,
                    "category", category.name()
            );

            // 2. JSON 변환
            String message = objectMapper.writeValueAsString(event);

            // 3. DB 트랜잭션 커밋 후 Kafka 발행
            if (TransactionSynchronizationManager.isActualTransactionActive()
                    && TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {
                                sendViewEvent(memberId, requestId, message);
                            }
                        }
                );

            } else {

                // 트랜잭션이 없는 경우 바로 발행
                sendViewEvent(memberId, requestId, message);
            }

        } catch (Exception e) {

            // Kafka 이벤트 생성 실패가 게시글 조회 실패로 이어지지 않도록 처리
            log.error(
                    "VIEW 이벤트 생성 실패 - memberId: {}, requestId: {}",
                    memberId,
                    requestId,
                    e
            );
        }
    }

    private void sendViewEvent(
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
                    "VIEW 이벤트 발행 요청 - memberId: {}, requestId: {}",
                    memberId,
                    requestId
            );

        } catch (Exception e) {

            log.error(
                    "VIEW 이벤트 발행 실패 - memberId: {}, requestId: {}",
                    memberId,
                    requestId,
                    e
            );
        }
    }

    // =====================================================
// Kafka REQUEST_CREATE 이벤트 발행
// =====================================================
    private void publishRequestCreateEvent(
            Long memberId,
            Long requestId,
            RequestCategory category
    ) {

        try {
            String eventId = UUID.randomUUID().toString();

            Map<String, Object> event = Map.of(
                    "eventId", eventId,
                    "userId", memberId,
                    "action", "REQUEST_CREATE",
                    "requestId", requestId,
                    "category", category.name()
            );

            String message = objectMapper.writeValueAsString(event);

            // DB 커밋 성공 후 Kafka 발행
            if (TransactionSynchronizationManager.isActualTransactionActive()
                    && TransactionSynchronizationManager.isSynchronizationActive()) {

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {
                                sendRequestCreateEvent(
                                        memberId,
                                        requestId,
                                        message
                                );
                            }
                        }
                );

            } else {
                sendRequestCreateEvent(
                        memberId,
                        requestId,
                        message
                );
            }

        } catch (Exception e) {

            log.error(
                    "REQUEST_CREATE 이벤트 생성 실패 - memberId: {}, requestId: {}",
                    memberId,
                    requestId,
                    e
            );
        }
    }

    // Kafka 전송
    private void sendRequestCreateEvent(
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
                    "REQUEST_CREATE 이벤트 발행 요청 - memberId: {}, requestId: {}",
                    memberId,
                    requestId
            );

        } catch (Exception e) {

            log.error(
                    "REQUEST_CREATE 이벤트 발행 실패 - memberId: {}, requestId: {}",
                    memberId,
                    requestId,
                    e
            );
        }
    }


}