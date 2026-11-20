package com.needitbe.domain.banner.service;

import com.needitbe.domain.banner.dto.AdBannerCreateRequest;
import com.needitbe.domain.banner.dto.AdBannerResponse;
import com.needitbe.domain.banner.dto.AdBannerUpdateRequest;
import com.needitbe.domain.banner.entity.AdBanner;
import com.needitbe.domain.banner.repository.AdBannerRepository;
import com.needitbe.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import com.needitbe.global.exception.CustomException;
import com.needitbe.global.exception.ErrorCode;
import com.needitbe.domain.campaign.entity.CampaignBanner;
import com.needitbe.domain.campaign.entity.CampaignStatus;
import com.needitbe.domain.campaign.repository.CampaignBannerRepository;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdBannerService {

    private final AdBannerRepository adBannerRepository;
    private final CampaignBannerRepository campaignBannerRepository;
    private final S3Service s3Service;

    // 광고 배너 등록
    // 광고 배너 등록
    @Transactional
    public AdBannerResponse createBanner(
            AdBannerCreateRequest request,
            MultipartFile image
    ) {

        // 1. S3 이미지 업로드
        String imageKey = s3Service.uploadBannerImage(image);

        // 2. DB 트랜잭션 롤백 시 업로드한 이미지 삭제
        registerRollbackCleanup(imageKey);

        // 3. 배너 엔티티 생성
        AdBanner banner = new AdBanner(
                request.name(),
                imageKey,
                request.targetUrl(),
                request.active()
        );

        // 4. DB 저장
        AdBanner savedBanner =
                adBannerRepository.saveAndFlush(banner);

        // 5. 이미지 조회 URL 생성
        String imageUrl =
                s3Service.getPresignedUrl(savedBanner.getImageKey());

        // 6. 응답 반환
        return AdBannerResponse.from(savedBanner, imageUrl);
    }

    // 전체 배너 목록 조회
    @Transactional(readOnly = true)
    public List<AdBannerResponse> getBanners() {

        return adBannerRepository.findAll().stream()
                .map(banner -> AdBannerResponse.from(
                        banner,
                        s3Service.getPresignedUrl(banner.getImageKey())
                ))
                .toList();
    }

    // 배너 상세 조회
    @Transactional(readOnly = true)
    public AdBannerResponse getBanner(Long bannerId) {

        AdBanner banner = adBannerRepository.findById(bannerId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.BANNER_NOT_FOUND)
                );

        return AdBannerResponse.from(
                banner,
                s3Service.getPresignedUrl(banner.getImageKey())
        );
    }

    // 광고 배너 수정
    @Transactional
    public AdBannerResponse updateBanner(
            Long bannerId,
            AdBannerUpdateRequest request
    ) {

        // 1. 배너 조회
        AdBanner banner = adBannerRepository.findById(bannerId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.BANNER_NOT_FOUND)
                );

        // 2. 기존 이미지 키 보관
        String oldImageKey = banner.getImageKey();
        String newImageKey = oldImageKey;

        // 3. 새 이미지가 첨부된 경우에만 업로드
        if (request.image() != null && !request.image().isEmpty()) {

            newImageKey = s3Service.uploadBannerImage(request.image());

            // DB 롤백 시 새 이미지 삭제
            registerRollbackCleanup(newImageKey);

            // DB 커밋 성공 시 기존 이미지 삭제
            registerCommitCleanup(oldImageKey);
        }

        // 4. 배너 정보 수정
        banner.update(
                request.name().trim(),
                newImageKey,
                request.targetUrl() == null
                        || request.targetUrl().isBlank()
                        ? null
                        : request.targetUrl().trim(),
                request.active()
        );

        // 5. DB 반영
        adBannerRepository.saveAndFlush(banner);

        // 6. 응답 반환
        return AdBannerResponse.from(
                banner,
                s3Service.getPresignedUrl(banner.getImageKey())
        );
    }

    // 광고 배너 삭제
    @Transactional
    public void deleteBanner(Long bannerId) {

        // 1. 삭제할 배너 조회
        AdBanner banner = adBannerRepository.findById(bannerId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.BANNER_NOT_FOUND)
                );

        // 2. 해당 배너가 연결된 캠페인 조회
        List<CampaignBanner> campaignBanners =
                campaignBannerRepository.findByBanner_Id(bannerId);

        // 3. 종료되지 않은 캠페인에서 사용 중인지 확인
        boolean isUsed = campaignBanners.stream()
                .anyMatch(campaignBanner -> {

                    CampaignStatus status =
                            campaignBanner.getCampaign().getStatus();

                    return status != CampaignStatus.ENDED;
                });

        // 4. 사용 중이라면 삭제 차단
        if (isUsed) {
            throw new CustomException(ErrorCode.BANNER_IN_USE);
        }

        // 5. S3 이미지 키 보관
        String imageKey = banner.getImageKey();

        // 6. 캠페인과 배너 연결 정보 삭제
        campaignBannerRepository.deleteAll(campaignBanners);
        campaignBannerRepository.flush();

        // 7. DB에서 배너 삭제
        adBannerRepository.delete(banner);
        adBannerRepository.flush();

        // 8. DB 커밋 성공 후 S3 이미지 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {
                            s3Service.deleteImage(imageKey);

                        } catch (RuntimeException e) {

                            log.error(
                                    "배너 S3 이미지 삭제 실패: {}",
                                    imageKey,
                                    e
                            );
                        }
                    }
                }
        );
    }

    // DB 롤백 시 새로 업로드한 S3 이미지 삭제
    private void registerRollbackCleanup(String imageKey) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            deleteImageSafely(imageKey);
                        }
                    }
                }
        );
    }

    // DB 커밋 성공 시 기존 S3 이미지 삭제
    private void registerCommitCleanup(String imageKey) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        deleteImageSafely(imageKey);
                    }
                }
        );
    }

    // S3 이미지 삭제 실패 시 로그 기록
    private void deleteImageSafely(String imageKey) {

        try {
            s3Service.deleteImage(imageKey);

            log.info("S3 이미지 삭제 완료: {}", imageKey);

        } catch (RuntimeException e) {

            log.error(
                    "S3 이미지 삭제 실패: {}",
                    imageKey,
                    e
            );
        }
    }
}