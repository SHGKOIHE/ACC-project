package com.foodgroup.common.exception;

import com.foodgroup.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("BusinessException: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT.getCode(), message, req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e, HttpServletRequest req) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT.getCode(), e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.OPTIMISTIC_LOCK.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.OPTIMISTIC_LOCK.getCode(), ErrorCode.OPTIMISTIC_LOCK.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), e);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage(), req.getRequestURI()));
    }
}
