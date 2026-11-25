package com.vibenote.common.exception;

import com.vibenote.common.web.JsonBodyLimitFilter;
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
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.exc.StreamConstraintsException;

/** 모든 예외를 {@code {code, message, details}} 형식으로 변환한다. 스택 트레이스는 응답에 넣지 않는다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(new ErrorResponse(code.name(), e.getMessage(), e.getDetails()));
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

    /**
     * 본문을 읽지 못한 경우. JSON 본문 바이트 한도({@link JsonBodyLimitFilter}, #73)나 Jackson 읽기 한도(본문 1MB 등,
     * application.yml)를 넘었으면 413, 나머지는 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
            if (t instanceof StreamConstraintsException
                    || t instanceof JsonBodyLimitFilter.BodyTooLargeException) {
                return respond(ErrorCode.COMMON_413, null);
            }
        }
        return respond(ErrorCode.COMMON_400, null);
    }

    @ExceptionHandler({
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

    /** 업로드 요청이 multipart가 아니거나 깨진 경우 (크기 초과는 위의 413 핸들러가 먼저 잡는다). */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException e) {
        return respond(ErrorCode.COMMON_400, null);
    }

    /**
     * 나머지 예외. 위에서 따로 다루지 않은 Spring MVC 예외(415 Content-Type, 필수 헤더 누락, ResponseStatusException 등)는
     * Spring의 {@link org.springframework.web.ErrorResponse}를 구현하므로 그 상태 코드에 맞는 공통 코드로 바꾼다. 4xx가
     * 500으로 바뀌어 ERROR 로그를 남기지 않게 한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        ErrorCode code = ErrorCode.COMMON_500;
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            int status = springError.getStatusCode().value();
            code =
                    switch (status) {
                        case 401 -> ErrorCode.AUTH_401;
                        case 403 -> ErrorCode.AUTH_403;
                        case 404 -> ErrorCode.COMMON_404;
                        case 405 -> ErrorCode.COMMON_405;
                        case 413 -> ErrorCode.COMMON_413;
                        case 415 -> ErrorCode.COMMON_415;
                        default -> status < 500 ? ErrorCode.COMMON_400 : ErrorCode.COMMON_500;
                    };
        }
        if (code == ErrorCode.COMMON_500) {
            log.error("처리되지 않은 예외", e);
        }
        return respond(code, null);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, Map<String, String> details) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, details));
    }
}
