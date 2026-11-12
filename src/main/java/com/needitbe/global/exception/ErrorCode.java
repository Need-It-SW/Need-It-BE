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
    // 회원 차단 오류
    MEMBER_BLOCKED(HttpStatus.FORBIDDEN, "MEMBER_403_1", "차단 관계에 있는 회원과는 메시지를 주고받을 수 없습니다."),
    OFFER_BLOCKED(HttpStatus.FORBIDDEN, "OFFER_403_1", "차단 관계에 있는 회원에게는 나눔 제안을 보낼 수 없습니다."),
    OFFER_BLOCKED_ACCEPT(HttpStatus.FORBIDDEN, "OFFER_403_2", "차단 관계에 있는 회원의 제안은 수락할 수 없습니다."),


    // 인증 오류
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "유효하지 않은 토큰입니다."),

    // 게시글 오류
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "REQUEST_404_1", "요청 게시글을 찾을 수 없습니다."),
    REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "REQUEST_403_1", "해당 게시글을 수정할 권한이 없습니다."),
    REQUEST_ALREADY_CLOSED(HttpStatus.BAD_REQUEST,"REQUEST_400_1","이미 마감된 요청 게시글입니다."),
    REQUEST_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND,"REQUEST_IMAGE_404_1","요청 게시글 이미지를 찾을 수 없습니다."),
    REQUEST_HAS_ACCEPTED_OFFER(HttpStatus.CONFLICT, "REQUEST_HAS_ACCEPTED_OFFER", "수락된 나눔 제안이 있습니다. 수락을 취소하거나 거래를 완료한 후 게시글을 마감해주세요."),
    REQUEST_HAS_OFFERS(HttpStatus.CONFLICT, "REQUEST_HAS_OFFERS", "나눔 제안이 존재하는 게시글은 삭제할 수 없습니다."),

    //제안 오류
    OFFER_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "OFFER_400_1", "본인의 요청 게시글에는 제안할 수 없습니다."),
    OFFER_REQUEST_CLOSED(HttpStatus.BAD_REQUEST, "OFFER_400_2", "마감된 요청 게시글에는 제안할 수 없습니다."),
    OFFER_NOT_FOUND(HttpStatus.NOT_FOUND, "OFFER_001", "제안을 찾을 수 없습니다."),
    OFFER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "OFFER_002", "제안을 처리할 권한이 없습니다."),
    OFFER_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "OFFER_003", "이미 처리된 제안입니다."),
    OFFER_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "OFFER_004", "이미 수락된 제안이 존재합니다."),
    OFFER_NOT_ACCEPTED(HttpStatus.BAD_REQUEST, "OFFER_005", "수락된 제안만 거래 완료할 수 있습니다."),
    OFFER_ALREADY_COMPLETED(HttpStatus.CONFLICT, "OFFER_006", "이미 완료된 거래입니다."),


    //채팅 오류
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT_001","채팅방을 찾을 수 없습니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_002","채팅방에 접근할 권한이 없습니다."),
    CHAT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CHAT_003","현재 메시지를 전송할 수 없는 채팅방입니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
