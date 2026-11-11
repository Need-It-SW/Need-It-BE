package com.needitbe.domain.request.controller;

import com.needitbe.domain.request.dto.RequestCreateRequest;
import com.needitbe.domain.request.dto.RequestImageResponse;
import com.needitbe.domain.request.dto.RequestResponse;
import com.needitbe.domain.request.dto.RequestUpdateRequest;
import com.needitbe.domain.request.service.RequestService;
import com.needitbe.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.needitbe.domain.request.entity.RequestCategory;
import com.needitbe.domain.request.entity.RequestStatus;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    // 요청 게시글 작성
    @PostMapping
    public ResponseEntity<ApiResponse<RequestResponse>> createRequest(
            Authentication authentication,
            @Valid @RequestBody RequestCreateRequest request
    ) {

        // JWT에서 인증된 회원 ID 추출
        Long memberId = Long.valueOf(authentication.getName());

        // 게시글 작성
        RequestResponse response =
                requestService.createRequest(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "요청 게시글 작성에 성공했습니다.",
                                response
                        )
                );
    }

    // =====================================================
// 요청 게시글 목록 조회
// =====================================================
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RequestResponse>>> getRequests(

            Authentication authentication,

            @RequestParam(required = false)
            String region,

            @RequestParam(required = false)
            RequestCategory category,

            @RequestParam(required = false)
            RequestStatus status,

            @RequestParam(defaultValue = "latest")
            String sort,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size
    ) {

        Page<RequestResponse> response;

        // 로그인 사용자
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            Long memberId = Long.valueOf(authentication.getName());

            response = requestService.getRequests(
                    memberId,
                    region,
                    category,
                    status,
                    sort,
                    page,
                    size
            );

        } else {

            // 비로그인 사용자
            response = requestService.getRequests(
                    region,
                    category,
                    status,
                    sort,
                    page,
                    size
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "요청 게시글 목록 조회에 성공했습니다.",
                        response
                )
        );
    }

    // 요청 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestResponse>> getRequest(
            @PathVariable("id") Long requestId,
            Authentication authentication
    ) {

        RequestResponse response;

        // 로그인한 사용자
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            Long memberId = Long.valueOf(authentication.getName());

            // 로그인한 회원의 관심 등록 여부 확인
            response = requestService.getRequest(
                    memberId,
                    requestId
            );

        } else {

            // 비로그인 사용자
            response = requestService.getRequest(requestId);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "요청 게시글 상세 조회에 성공했습니다.",
                        response
                )
        );
    }

    // 요청 게시글 수정
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestResponse>> updateRequest(
            Authentication authentication,
            @PathVariable("id") Long requestId,
            @Valid @RequestBody RequestUpdateRequest request
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        RequestResponse response = requestService.updateRequest(
                memberId,
                requestId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "요청 게시글 수정에 성공했습니다.",
                        response
                )
        );
    }

    // 요청 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(
            Authentication authentication,
            @PathVariable("id") Long requestId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        requestService.deleteRequest(memberId, requestId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "요청 게시글 삭제에 성공했습니다.",
                        null
                )
        );
    }

    // 요청 게시글 마감
    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<RequestResponse>> closeRequest(
            Authentication authentication,
            @PathVariable("id") Long requestId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        RequestResponse response =
                requestService.closeRequest(memberId, requestId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "요청 게시글 마감에 성공했습니다.",
                        response
                )
        );
    }

    // 내가 작성한 요청글 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<RequestResponse>>> getMyRequests(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100)
            int size
    ) {
        Long memberId = Long.valueOf(authentication.getName());

        Page<RequestResponse> response =
                requestService.getMyRequests(memberId, page, size);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내 요청 게시글 조회에 성공했습니다.",
                        response
                )
        );
    }

    @PostMapping(
            value = "/{id}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<RequestImageResponse>>> uploadImages(
            Authentication authentication,
            @PathVariable("id") Long requestId,
            @RequestPart("files") List<MultipartFile> files
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        List<RequestImageResponse> response =
                requestService.uploadImages(
                        memberId,
                        requestId,
                        files
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "게시글 이미지 업로드에 성공했습니다.",
                        response
                )
        );
    }

    // =====================================================
// 요청 게시글 개별 이미지 삭제
// =====================================================
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            Authentication authentication,
            @PathVariable("id") Long requestId,
            @PathVariable("imageId") Long imageId
    ) {

        Long memberId = Long.valueOf(authentication.getName());

        requestService.deleteImage(
                memberId,
                requestId,
                imageId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "게시글 이미지 삭제에 성공했습니다.",
                        null
                )
        );
    }
}