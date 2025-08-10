package com.example.bankcards.exception;

import java.time.OffsetDateTime;

public class ApiError {
    private final String code;
    private final String message;
    private final OffsetDateTime timestamp = OffsetDateTime.now();

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
