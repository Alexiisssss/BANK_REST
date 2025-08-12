package com.example.bankcards.exception;

import java.time.OffsetDateTime;

public class ApiError {
    private final String code;
    private final String message;
    private final String requestId;
    private final OffsetDateTime timestamp = OffsetDateTime.now();

    public ApiError(String code, String message, String requestId) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
