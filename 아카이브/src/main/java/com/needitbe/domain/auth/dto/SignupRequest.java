package com.needitbe.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]{4,32}$",
                message = "아이디는 영문, 숫자, 밑줄만 사용하여 4~32자로 입력해야 합니다."
        )
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{8,}$",
                message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 각각 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @NotBlank(message = "활동 지역은 필수입니다.")
        String region
) {
}