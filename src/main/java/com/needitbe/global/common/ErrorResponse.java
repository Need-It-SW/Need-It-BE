package com.needitbe.global.common;

import java.util.Map;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, String> errors
) {

    public static ErrorResponse of(
            String code,
            String message
    ) {
        return new ErrorResponse(
                false,
                code,
                message,
                Map.of()
        );
    }

    public static ErrorResponse of(
            String code,
            String message,
            Map<String, String> errors
    ) {
        return new ErrorResponse(
                false,
                code,
                message,
                errors
        );
    }
}