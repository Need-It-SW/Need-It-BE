package com.needitbe.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    // 이미지 업로드
    public String uploadImage(
            MultipartFile file,
            Long requestId
    ) {
        validateImage(file);

        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다."
            );
        };

        String imageKey = "requests/"
                + requestId
                + "/"
                + UUID.randomUUID()
                + extension;

        try {
            byte[] bytes = file.getBytes();

            PutObjectRequest putRequest =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(imageKey)
                            .contentType(file.getContentType())
                            .build();

            s3Client.putObject(
                    putRequest,
                    RequestBody.fromBytes(bytes)
            );

            return imageKey;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "이미지 파일을 읽는 데 실패했습니다.", e
            );
        }
    }

    // 이미지 삭제
    public void deleteImage(String imageKey) {

        DeleteObjectRequest deleteRequest =
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(imageKey)
                        .build();

        s3Client.deleteObject(deleteRequest);
    }

    // 이미지 조회용 임시 URL 생성
    public String getPresignedUrl(String imageKey) {

        GetObjectRequest getRequest =
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(imageKey)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(30))
                        .getObjectRequest(getRequest)
                        .build();

        return s3Presigner
                .presignGetObject(presignRequest)
                .url()
                .toString();
    }

    // 이미지 검증
    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "이미지 파일이 비어 있습니다."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "이미지 크기는 5MB를 초과할 수 없습니다."
            );
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "JPG, PNG, WEBP 이미지만 업로드할 수 있습니다."
            );
        }
    }
    public String getImageUrl(String imageKey) {
        return "https://" + bucket
                + ".s3.ap-northeast-2.amazonaws.com/"
                + imageKey;
    }

    public String uploadOfferImage(MultipartFile file, Long offerId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !(contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        };

        String imageKey = "offers/" + offerId + "/"
                + UUID.randomUUID() + "." + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(imageKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return imageKey;

        } catch (IOException e) {
            throw new IllegalStateException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    // 채팅 이미지 업로드
    // 채팅 이미지 업로드
    public String uploadChatImage(
            MultipartFile file,
            Long chatRoomId
    ) {
        validateImage(file);

        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다."
            );
        };

        String imageKey = "chats/"
                + chatRoomId
                + "/"
                + UUID.randomUUID()
                + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(imageKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return imageKey;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "채팅 이미지 업로드에 실패했습니다.", e
            );
        }
    }

    // 회원 프로필 이미지 업로드
    public String uploadProfileImage(
            MultipartFile file,
            Long memberId
    ) {
        validateImage(file);

        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다."
            );
        };

        String imageKey = "profiles/"
                + memberId
                + "/"
                + UUID.randomUUID()
                + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(imageKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return imageKey;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "프로필 이미지 업로드에 실패했습니다.", e
            );
        }
    }


}