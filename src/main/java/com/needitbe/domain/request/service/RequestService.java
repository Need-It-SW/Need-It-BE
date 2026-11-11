package com.needitbe.domain.request.service;

import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;

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

import lombok.extern.slf4j.Slf4j;

import com.needitbe.domain.offer.repository.OfferRepository;
import com.needitbe.domain.offer.entity.OfferStatus;
import com.needitbe.domain.favorite.repository.FavoriteRepository;
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

        // 1. 로그인한 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.MEMBER_NOT_FOUND)
                );

        // 2. 요청 게시글 생성
        ItemRequest itemRequest = new ItemRequest(
                member,
                request.title(),
                request.description(),
                request.category(),
                request.region()
        );

        // 3. MySQL 저장
        ItemRequest savedRequest =
                itemRequestRepository.save(itemRequest);

        // 4. 응답 DTO 반환
        return toResponse(savedRequest);
    }

    // =====================================================
    // 2. 요청 게시글 목록 조회
    // =====================================================
    @Transactional(readOnly = true)
    public Page<RequestResponse> getRequests(
            String region,
            RequestCategory category,
            RequestStatus status,
            int page,
            int size
    ) {

        // 1. 페이지네이션 + 최신순 정렬
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        // 2. 검색 조건 생성
        Specification<ItemRequest> specification =
                Specification.allOf(
                        ItemRequestSpecification.hasRegion(region),
                        ItemRequestSpecification.hasCategory(category),
                        ItemRequestSpecification.hasStatus(status)
                );

        // 3. DB 조회
        Page<ItemRequest> requests =
                itemRequestRepository.findAll(specification, pageable);

        // 4. DTO 변환
        return requests.map(this::toResponse);
    }

    // =====================================================
    // 3. 요청 게시글 상세 조회
    // =====================================================
    @Transactional(readOnly = true)
    public RequestResponse getRequest(Long requestId) {

        // 1. 게시글 ID로 조회
        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 2. 응답 DTO 반환
        return toResponse(itemRequest);
    }

    // =====================================================
    // 4. 요청 게시글 수정
    // =====================================================
    @Transactional
    public RequestResponse updateRequest(
            Long memberId,
            Long requestId,
            RequestUpdateRequest request
    ) {

        // 1. 게시글 조회
        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 2. 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 3. 게시글 수정
        itemRequest.update(
                request.title(),
                request.description(),
                request.category(),
                request.region()
        );

        // 4. 수정된 게시글 반환
        return toResponse(itemRequest);
    }

    // =====================================================
    // =====================================================
// =====================================================
// 5. 요청 게시글 삭제 (S3 이미지 함께 삭제)
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

        // 3. 제안 존재 여부 확인
        boolean hasOffers =
                offerRepository.existsByItemRequestId(requestId);

        // 4. 제안이 하나라도 존재하면 삭제 불가능
        if (hasOffers) {
            throw new CustomException(
                    ErrorCode.REQUEST_HAS_OFFERS
            );
        }

        // 5. 삭제할 S3 이미지 키 목록 확보
        List<String> imageKeys = itemRequest.getImages()
                .stream()
                .map(ItemRequestImage::getImageKey)
                .toList();

        // 6. 게시글 및 연결된 이미지 DB 삭제
        // 6. 해당 게시글에 등록된 관심 데이터 삭제
        favoriteRepository.deleteByItemRequestId(requestId);

// 7. 게시글 및 연결된 이미지 DB 삭제
        itemRequestRepository.delete(itemRequest);

        // 7. DB 커밋 성공 후 S3 이미지 삭제
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
// 6. 요청 게시글 마감
// =====================================================
    @Transactional
    public RequestResponse closeRequest(
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

        // 3. 이미 마감된 게시글인지 확인
        if (itemRequest.getStatus() == RequestStatus.CLOSED) {
            throw new CustomException(ErrorCode.REQUEST_ALREADY_CLOSED);
        }

        // 4. 수락된 제안이 있는지 확인
        boolean hasAcceptedOffer =
                offerRepository.existsByItemRequestIdAndStatus(
                        requestId,
                        OfferStatus.ACCEPTED
                );

        // 5. 수락된 제안이 있다면 직접 마감 불가능
        if (hasAcceptedOffer) {
            throw new CustomException(
                    ErrorCode.REQUEST_HAS_ACCEPTED_OFFER
            );
        }

        // 6. 요청 게시글 마감
        itemRequest.close();

        // 7. 결과 반환
        return toResponse(itemRequest);
    }

    // =====================================================
    // 7. 내가 작성한 요청 게시글 조회
    // =====================================================
    @Transactional(readOnly = true)
    public Page<RequestResponse> getMyRequests(
            Long memberId,
            int page,
            int size
    ) {

        // 1. 페이지네이션 + 최신순 정렬
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        // 2. 내가 작성한 게시글 조회
        Page<ItemRequest> requests =
                itemRequestRepository.findByMemberId(memberId, pageable);

        // 3. DTO 변환
        return requests.map(this::toResponse);
    }

    // =====================================================
    // 8. 요청 게시글 이미지 업로드
    // =====================================================
    @Transactional
    public List<RequestImageResponse> uploadImages(
            Long memberId,
            Long requestId,
            List<MultipartFile> files
    ) {

        // 1. 게시글 조회
        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 2. 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 3. 이미지 개수 확인
        int currentCount = itemRequest.getImages().size();

        if (files == null || files.isEmpty()
                || currentCount + files.size() > 5) {
            throw new IllegalArgumentException(
                    "이미지는 게시글당 최대 5장까지 등록할 수 있습니다."
            );
        }

        // 4. S3 업로드
        List<String> uploadedKeys = new ArrayList<>();

        try {

            for (int i = 0; i < files.size(); i++) {

                // S3에 이미지 업로드
                String imageKey = s3Service.uploadImage(
                        files.get(i),
                        requestId
                );

                uploadedKeys.add(imageKey);

                // S3 이미지 URL 생성
                String imageUrl = s3Service.getImageUrl(imageKey);

                // 게시글에 이미지 추가
                itemRequest.addImage(
                        imageUrl,
                        imageKey,
                        currentCount + i
                );
            }

            // 5. MySQL 저장
            itemRequestRepository.saveAndFlush(itemRequest);

            // 6. Presigned URL을 포함한 응답 반환
            return itemRequest.getImages()
                    .stream()
                    .map(image -> RequestImageResponse.from(
                            image,
                            s3Service.getPresignedUrl(image.getImageKey())
                    ))
                    .toList();

        } catch (Exception e) {

            // 업로드 중 오류 발생 시 S3 파일 정리
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
    // 9. ItemRequest -> RequestResponse 변환
    // =====================================================
    private RequestResponse toResponse(ItemRequest request) {

        // 게시글 이미지 목록을 Presigned URL로 변환
        List<RequestImageResponse> images = request.getImages()
                .stream()
                .map(image -> RequestImageResponse.from(
                        image,
                        s3Service.getPresignedUrl(image.getImageKey())
                ))
                .toList();

        // 게시글 정보 + 이미지 목록 반환
        return RequestResponse.from(request, images);
    }

    // =====================================================
// 10. 요청 게시글 개별 이미지 삭제
// =====================================================
    @Transactional
    public void deleteImage(
            Long memberId,
            Long requestId,
            Long imageId
    ) {

        // 1. 게시글 조회
        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REQUEST_NOT_FOUND)
                );

        // 2. 작성자 확인
        if (!itemRequest.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        // 3. 해당 게시글의 이미지 조회
        ItemRequestImage image =
                itemRequestImageRepository
                        .findByIdAndItemRequestId(imageId, requestId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.REQUEST_IMAGE_NOT_FOUND
                                )
                        );

        // 4. S3 객체 키 보관
        String imageKey = image.getImageKey();

        // 5. 게시글 이미지 목록에서 제거
        itemRequest.removeImage(image);

        // 6. DB 커밋 성공 후 S3 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            s3Service.deleteImage(imageKey);
                        } catch (Exception e) {
                            // S3 삭제 실패 시 로그 기록
                            // 운영 환경에서는 재시도 작업이 필요함
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

    // 로그인 회원의 관심 여부를 포함한 게시글 상세 조회
    @Transactional(readOnly = true)
    public RequestResponse getRequest(
            Long memberId,
            Long requestId
    ) {

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

        // 3. 이미지 응답 변환
        List<RequestImageResponse> images = itemRequest.getImages()
                .stream()
                .map(image -> RequestImageResponse.from(
                        image,
                        s3Service.getPresignedUrl(image.getImageKey())
                ))
                .toList();

        // 4. 관심 여부를 포함한 응답 반환
        return RequestResponse.from(
                itemRequest,
                images,
                isFavorite
        );
    }

    @Transactional(readOnly = true)
    public Page<RequestResponse> getRequests(
            Long memberId,
            String region,
            RequestCategory category,
            RequestStatus status,
            int page,
            int size
    ) {

        // 1. 페이지네이션 + 최신순 정렬
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        // 2. 검색 조건
        Specification<ItemRequest> specification =
                Specification.allOf(
                        ItemRequestSpecification.hasRegion(region),
                        ItemRequestSpecification.hasCategory(category),
                        ItemRequestSpecification.hasStatus(status)
                );

        // 3. 게시글 목록 조회
        Page<ItemRequest> requests =
                itemRequestRepository.findAll(specification, pageable);

        // 4. 현재 페이지의 게시글 ID 수집
        List<Long> requestIds = requests.getContent()
                .stream()
                .map(ItemRequest::getId)
                .toList();

        // 5. 관심 등록된 게시글 ID 조회
        Set<Long> favoriteIds = requestIds.isEmpty()
                ? Set.of()
                : new HashSet<>(
                favoriteRepository.findFavoriteRequestIds(
                        memberId,
                        requestIds
                )
        );

        // 6. DTO 변환
        return requests.map(request -> {

            boolean isFavorite = favoriteIds.contains(request.getId());

            List<RequestImageResponse> images = request.getImages()
                    .stream()
                    .map(image -> RequestImageResponse.from(
                            image,
                            s3Service.getPresignedUrl(image.getImageKey())
                    ))
                    .toList();

            return RequestResponse.from(
                    request,
                    images,
                    isFavorite
            );
        });
    }
}