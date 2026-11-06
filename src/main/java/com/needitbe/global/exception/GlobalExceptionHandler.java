package com.needitbe.global.exception;

import com.needitbe.global.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            CustomException e
    ) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage() != null
                                        ? error.getDefaultMessage()
                                        : "올바르지 않은 값입니다."
                        )
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "COMMON_400",
                        "입력값 검증에 실패했습니다.",
                        errors
                ));
    }

    // 예상하지 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ) {

        // 서버 로그에만 상세 오류 기록
        // 실제 운영에서는 Logger 사용 권장
        // log.error("Unexpected error", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        "COMMON_500",
                        "서버 내부 오류가 발생했습니다."
                ));
    }
}