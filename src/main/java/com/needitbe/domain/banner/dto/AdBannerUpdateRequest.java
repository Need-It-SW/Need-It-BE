package com.needitbe.domain.banner.dto;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

public record AdBannerUpdateRequest(

        @NotBlank(message = "배너 이름은 필수입니다.")

        @Size(max = 100)

        String name,

        @Size(max = 1000)

        String targetUrl,

        boolean active,

        // 이미지 변경 시에만 첨부

        MultipartFile image

) {

}