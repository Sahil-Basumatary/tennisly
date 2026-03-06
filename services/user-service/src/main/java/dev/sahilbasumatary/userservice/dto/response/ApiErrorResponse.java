package dev.sahilbasumatary.userservice.dto.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, message, path, Instant.now(), null);
    }

    public static ApiErrorResponse withFieldErrors(
            int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ApiErrorResponse(status, error, message, path, Instant.now(), fieldErrors);
    }
}
