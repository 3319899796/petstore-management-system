package com.ningcong.petstore.common;

/**
 * 业务异常：service 层发现"业务规则不允许"时抛出，由 GlobalExceptionHandler 统一翻译成响应体。
 *
 * <p>不继承受检异常，避免每层都写 try-catch；用 code 区分 4xx/5xx。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.PARAM_ERROR.getCode(), message);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
