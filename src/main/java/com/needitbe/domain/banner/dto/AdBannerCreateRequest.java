package com.needitbe.domain.banner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record AdBannerCreateRequest(

        @NotBlank(message = "배너 이름은 필수입니다.")
        @Size(max = 100)
        String name,

        @Size(max = 1000)
        String targetUrl,

        boolean active,

        @NotNull(message = "배너 이미지는 필수입니다.")
        MultipartFile image

) {
}