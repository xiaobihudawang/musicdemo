package org.example.musicdemo.common;

/**
 * 响应状态码枚举，定义系统中所有标准化的业务状态码与提示消息。
 *
 * <p>此枚举与 {@link Result} 配合使用，确保整个项目的 API 响应使用统一的、
 * 可维护的状态码体系。每个枚举常量包含两个属性：</p>
 * <ul>
 *   <li><b>code</b>（int）：数字状态码，供前端做条件判断（如 if(code === 401) 跳转登录页）。</li>
 *   <li><b>message</b>（String）：默认的中文提示消息，供前端直接展示或作为兜底文案。</li>
 * </ul>
 *
 * <p>状态码设计原则：</p>
 * <ul>
 *   <li>与 HTTP 状态码保持语义一致，但不是直接映射——本系统所有接口 HTTP 状态码固定为 200，
 *       业务是否成功由返回 JSON 中的 code 字段决定。</li>
 *   <li>200 系列代表成功，400 系列代表客户端错误，500 系列代表服务端错误。</li>
 *   <li>需要新增业务错误码时，在此枚举中添加新的常量即可。</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 在 Controller/Service 中直接引用
 * return Result.fail(ResultCode.UNAUTHORIZED);
 * // 输出：{"code": 401, "message": "请先登录", "data": null}
 * }</pre>
 *
 * @see org.example.musicdemo.common.Result
 */
public enum ResultCode {

    /**
     * 200 — 操作成功。
     * 所有业务处理正常完成时的通用状态码。
     */
    SUCCESS(200, "操作成功"),

    /**
     * 400 — 操作失败。
     * 通用业务错误，通常用于参数校验不通过、业务规则冲突等场景。
     * 需要在 message 中给出具体失败原因。
     */
    FAIL(400, "操作失败"),

    /**
     * 401 — 请先登录。
     * 当用户未提供有效的 JWT 令牌，或令牌已过期时返回此状态码。
     * 前端在收到 401 后可自动跳转到登录页。
     */
    UNAUTHORIZED(401, "请先登录"),

    /**
     * 403 — 权限不足。
     * 用户已登录但角色权限不足以访问目标资源时返回。
     * 例如普通用户尝试访问 /api/admin/** 接口时触发。
     */
    FORBIDDEN(403, "权限不足"),

    /**
     * 404 — 资源不存在。
     * 查询的音乐、评论等资源在数据库中不存在时返回。
     * 与 HTTP 404 语义一致，但通过业务 code 返回而非 HTTP 状态码。
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 500 — 服务器内部错误。
     * 发生未预料的运行时异常时返回此状态码，同时 message 不会暴露具体异常栈信息，
     * 而是使用通用的 "服务器内部错误"，防止敏感信息泄露。
     * 详细的异常日志记录在服务端日志文件中。
     */
    INTERNAL_ERROR(500, "服务器内部错误");

    /** 数字状态码，与 HTTP 状态码保持语义一致 */
    private final int code;

    /** 默认提示消息，用中文展示给用户 */
    private final String message;

    /**
     * 枚举构造器，为每个常量绑定状态码和消息。
     *
     * @param code    数字状态码
     * @param message 默认提示消息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取数字状态码。
     *
     * @return 状态码值，如 200、400、401 等
     */
    public int getCode() { return code; }

    /**
     * 获取默认提示消息。
     *
     * @return 中文提示消息，如 "操作成功"、"请先登录"
     */
    public String getMessage() { return message; }
}
