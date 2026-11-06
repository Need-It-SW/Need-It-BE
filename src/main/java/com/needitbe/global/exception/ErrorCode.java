package com.needitbe.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 오류
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),

    // 회원 오류
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_409_1", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "MEMBER_409_2", "이미 사용 중인 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "MEMBER_409_3", "이미 사용 중인 아이디입니다."),
    SAME_REGION(HttpStatus.BAD_REQUEST, "MEMBER_400_1", "이미 설정된 활동 동네입니다."),
    SAME_NICKNAME(HttpStatus.BAD_REQUEST, "MEMBER_400_2", "이미 설정된 닉네임입니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER_400_3", "현재 비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER_400_4", "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."),
    NEW_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_400_5", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."),


    // 인증 오류
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "유효하지 않은 토큰입니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;
}
