package com.fluffycat.sentinelapp.common.exception;

import com.fluffycat.sentinelapp.common.api.ErrorCode;
import com.fluffycat.sentinelapp.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleValidException(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.VALIDATION_FAILED.getDefaultMessage();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(Result.error(ErrorCode.VALIDATION_FAILED,msg));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        // 记录内部日志，但对外返回受控 message
        log.warn("BusinessException: code={}, msg={}", code.getCode(), e.getMessage());

        return ResponseEntity
                .status(code.getHttpStatus())
                .body(Result.error(code, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        // 内部保留堆栈
        log.error("Unhandled exception", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(Result.error(ErrorCode.INTERNAL_ERROR));
    }
}
