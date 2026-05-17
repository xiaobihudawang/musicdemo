package org.example.musicdemo.common;

/**
 * 响应状态码
 * 200：成功
 * 400：请求错误
 * 401：未认证
 * 403：无权限
 * 500：服务器错误
 */
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(400, "操作失败"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
