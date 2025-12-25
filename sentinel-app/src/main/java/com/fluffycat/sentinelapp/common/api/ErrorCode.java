package com.fluffycat.sentinelapp.common.api;


import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 0 表示成功
    OK(0, HttpStatus.OK, "success"),

    // 4xx：客户端/业务类错误
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST, "请求参数不合法"),
    VALIDATION_FAILED(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(40900, HttpStatus.CONFLICT, "资源冲突"),
    UNPROCESSABLE(42200, HttpStatus.UNPROCESSABLE_ENTITY, "业务处理失败"),

    // 5xx：服务端错误
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试"),
    DEPENDENCY_ERROR(50010, HttpStatus.BAD_GATEWAY, "依赖服务异常");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;


    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
