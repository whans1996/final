package com.vibenote.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 모든 예외를 {@code {code, message, details}} 형식으로 변환한다. 스택 트레이스는 응답에 넣지 않는다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return respond(e.getErrorCode(), e.getDetails());
    }

    /**
     * @Valid 본문 검증 실패와 @ModelAttribute 바인딩 실패 (MethodArgumentNotValidException은 BindException의 하위
     * 타입).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getFieldErrors()
                .forEach(fe -> details.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return respond(ErrorCode.COMMON_400, details.isEmpty() ? null : details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getParameterValidationResults()
                .forEach(
                        r ->
                                r.getResolvableErrors()
                                        .forEach(
                                                err ->
                                                        details.putIfAbsent(
                                                                r.getMethodParameter()
                                                                        .getParameterName(),
                                                                err.getDefaultMessage())));
        return respond(ErrorCode.COMMON_400, details.isEmpty() ? null : details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getConstraintViolations()
                .forEach(v -> details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return respond(ErrorCode.COMMON_400, details.isEmpty() ? null : details);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return respond(ErrorCode.COMMON_400, null);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
        return respond(ErrorCode.COMMON_404, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.COMMON_405, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException e) {
        return respond(ErrorCode.COMMON_413, null);
    }

    /** 메서드 보안(@PreAuthorize)에서 던진 예외가 500으로 바뀌지 않게 한다. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return respond(ErrorCode.AUTH_403, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return respond(ErrorCode.AUTH_401, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.COMMON_500, null);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, Map<String, String> details) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, details));
    }
}
