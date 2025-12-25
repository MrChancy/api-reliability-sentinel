package com.fluffycat.sentinelapp.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Result<T> {
    private int code;        // 0 = success, 非 0 = errorCode
    private String message;
    private T data;
    private String traceId; // 用于排障（后续接入 MDC）

    public static <T> Result<T> success(T data){
        return Result.<T>builder()
                .code(ErrorCode.OK.getCode())
                .message(ErrorCode.OK.getDefaultMessage())
                .data(data)
                .build();
    }

    public static Result<?> error(ErrorCode errorCode) {
        return Result.builder()
                .code(errorCode.getCode())
                .message(errorCode.getDefaultMessage())
                .build();
    }

    public static Result<?> error(ErrorCode errorCode, String messageOverride) {
        return Result.builder()
                .code(errorCode.getCode())
                .message(messageOverride)
                .build();
    }
}
