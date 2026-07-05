package dev.sahilbasumatary.replayservice.exception;

import dev.sahilbasumatary.replayservice.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    @ExceptionHandler(ReplayGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationFailure(
            ReplayGenerationException ex, HttpServletRequest request) {
        log.warn("Replay generation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                                "Unprocessable Entity",
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleDownstreamFailure(
            DownstreamServiceException ex, HttpServletRequest request) {
        log.error("Downstream dependency failed on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.BAD_GATEWAY.value(),
                                "Bad Gateway",
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    @ExceptionHandler(ReplayStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageFailure(
            ReplayStorageException ex, HttpServletRequest request) {
        log.error("Replay storage failure on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "Service Unavailable",
                                "Replay storage is currently unavailable",
                                request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                fe ->
                                        new ApiErrorResponse.FieldError(
                                                fe.getField(), fe.getDefaultMessage()))
                        .toList();
        log.warn("Validation failed on {} field(s)", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiErrorResponse.withFieldErrors(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Failed",
                                "Request contains invalid parameters",
                                request.getRequestURI(),
                                fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "An unexpected error occurred",
                                request.getRequestURI()));
    }
}
