package com.infy.pinterest.utility;


import com.infy.pinterest.dto.ApiResponse;
import com.infy.pinterest.exception.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handlePasswordMismatch(PasswordMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<AccountLockedResponse> handleAccountLocked(AccountLockedException ex) {
        AccountLockedResponse response = new AccountLockedResponse(
                "error",
                ex.getMessage(),
                ex.getRetryAfter(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(new ValidationError(fieldName, errorMessage));
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                "error",
                "Validation failed",
                LocalDateTime.now(),
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }

    @Data
    @AllArgsConstructor
    static class ValidationErrorResponse {
        private String status;
        private String message;
        private LocalDateTime timestamp;
        private List<ValidationError> errors;
    }

    @Data
    @AllArgsConstructor
    static class ValidationError {
        private String field;
        private String message;
    }

    @Data
    @AllArgsConstructor
    static class AccountLockedResponse {
        private String status;
        private String message;
        private Integer retryAfter;
        private LocalDateTime timestamp;
    }

    @ExceptionHandler(AlreadyFollowingException.class)
    public ResponseEntity<ApiResponse<Object>> handleAlreadyFollowing(AlreadyFollowingException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NotFollowingException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFollowing(NotFollowingException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvitationNotFound(InvitationNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ApiResponse<Object>> handleSelfFollow(SelfFollowException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleBusinessProfileNotFound(BusinessProfileNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessProfileAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleBusinessProfileAlreadyExists(BusinessProfileAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleBoardNotFound(BoardNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PinNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePinNotFound(PinNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileUpload(FileUploadException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        // Handle blocked user related exceptions with appropriate status codes
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        if (message != null) {
            if (message.contains("not blocked")) {
                status = HttpStatus.BAD_REQUEST;
            } else if (message.contains("already blocked")) {
                status = HttpStatus.CONFLICT;
            } else if (message.contains("Cannot block yourself")) {
                status = HttpStatus.BAD_REQUEST;
            } else if (message.contains("has blocked you")) {
                status = HttpStatus.FORBIDDEN;
            }
        }
        
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(message != null ? message : "An error occurred"));
    }


}
