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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

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
                        ItemRequestSpecification.hasRegion(region),
                        ItemRequestSpecification.hasCategory(category),
                        ItemRequestSpecification.hasStatus(status),
                        ItemRequestSpecification.hasKeyword(keyword)
                );

        // 게시글 목록 조회
        Page<ItemRequest> requests =
                itemRequestRepository.findAll(specification, pageable);

        // 비로그인 사용자는 isFavorite = false
        return requests.map(this::toResponse);
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
        return requests.map(request -> {

            boolean isFavorite =
                    favoriteIds.contains(request.getId());

            return toResponse(request, isFavorite);
        });
    }

    // =====================================================
    // 4. 요청 게시글 상세 조회 (비로그인)
    // =====================================================
    @Transactional(readOnly = true)
    public RequestResponse getRequest(Long requestId) {

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        return toResponse(itemRequest);
    }

    // =====================================================
    // 5. 요청 게시글 상세 조회 (로그인)
    // =====================================================
    @Transactional(readOnly = true)
    public RequestResponse getRequest(
            Long memberId,
            Long requestId
    ) {

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 관심 등록 여부 확인
        boolean isFavorite =
                favoriteRepository.existsByMemberIdAndItemRequestId(
                        memberId,
                        requestId
                );

        return toResponse(itemRequest, isFavorite);
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

        // 게시글 조회 및 잠금
        ItemRequest itemRequest =
                itemRequestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() ->
                                new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                        );

        // 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 제안 존재 여부 확인
        boolean hasOffers =
                offerRepository.existsByItemRequestId(requestId);

        // 제안이 하나라도 있으면 삭제 불가능
        if (hasOffers) {
            throw new CustomException(
                    ErrorCode.REQUEST_HAS_OFFERS
            );
        }

        // 삭제할 S3 이미지 키 목록 확보
        List<String> imageKeys = itemRequest.getImages()
                .stream()
                .map(ItemRequestImage::getImageKey)
                .toList();

        // 연결된 관심 게시글 데이터 삭제
        favoriteRepository.deleteByItemRequestId(requestId);

        // 게시글 및 이미지 DB 삭제
        itemRequestRepository.delete(itemRequest);

        // DB 커밋 성공 후 S3 이미지 삭제
        if (!imageKeys.isEmpty()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {

                            for (String imageKey : imageKeys) {
                                try {

                                    s3Service.deleteImage(imageKey);

                                    log.info(
                                            "S3 이미지 삭제 성공. imageKey={}",
                                            imageKey
                                    );

                                } catch (Exception e) {

                                    log.error(
                                            "S3 이미지 삭제 실패. imageKey={}",
                                            imageKey,
                                            e
                                    );
                                }
                            }
                        }
                    }
            );
        }
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
                itemRequestRepository.findByMemberId(
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
                isFavorite
        );
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
}