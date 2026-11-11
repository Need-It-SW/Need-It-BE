package com.needitbe.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsernameUpdateRequest(

        @NotBlank(message = "변경할 아이디를 입력해주세요.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]{4,32}$",
                message = "아이디는 영문, 숫자, 밑줄(_)을 사용하여 4~32자로 입력해야 합니다."
        )
        String username,

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword

) {
}