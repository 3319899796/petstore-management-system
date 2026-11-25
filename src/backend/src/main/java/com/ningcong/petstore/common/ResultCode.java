package com.ningcong.petstore.common;

/**
 * 业务状态码。与 HTTP 状态码保持一致，便于前端拦截器统一处理。
 */
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数不合法"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
