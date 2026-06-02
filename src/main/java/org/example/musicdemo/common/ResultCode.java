package org.example.musicdemo.common;

/**
 * 业务状态码枚举，与 Result 配合使用。
 * 每个枚举包含数字状态码和默认提示消息。
 */
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 操作失败（通用业务错误） */
    FAIL(400, "操作失败"),

    /** 未登录或 Token 过期 */
    UNAUTHORIZED(401, "请先登录"),

    /** 权限不足 */
    FORBIDDEN(403, "权限不足"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 服务器内部错误 */
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
