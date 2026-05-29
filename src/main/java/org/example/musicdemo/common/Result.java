package org.example.musicdemo.common;
import lombok.Data;
/**
 * 统一 API 返回格式。
 *
 * <p>系统中所有 Controller 的接口返回值都必须包装为此对象，
 * 确保前端收到的每个响应都遵循相同的 JSON 结构：
 * {@code {"code": 200, "message": "操作成功", "data": {...}}} 。</p>
 *
 * <p>这样设计的优势：</p>
 * <ul>
 *   <li><b>统一错误处理</b>：无论业务成功或失败，前端都可以用同一套逻辑解析响应。
 *       通过 code 字段判断结果状态，而非 HTTP 状态码或 try-catch。</li>
 *   <li><b>类型安全</b>：使用泛型 {@code T} 约束 data 字段的类型，
 *       便于 Jackson 序列化时保留准确的类型信息。</li>
 *   <li><b>静态工厂方法</b>：提供 {@link #success()}、{@link #success(Object)}、
 *       {@link #fail(String)} 等静态方法，在 Controller 和 Service 中可以非常简洁地构造返回值，
 *       避免到处手动 new Result。</li>
 * </ul>
 *
 * <p>使用示例（Controller 中）：</p>
 * <pre>{@code
 * // 查询成功，返回数据列表
 * return Result.success(musicList);
 *
 * // 参数校验失败，返回错误信息
 * return Result.fail("音乐标题不能为空");
 *
 * // 未登录
 * return Result.fail(ResultCode.UNAUTHORIZED);
 * }</pre>
 *
 * @param <T> data 字段的实际数据类型
 * @see org.example.musicdemo.common.ResultCode
 */
@Data
public class Result<T> {

    /**
     * 业务状态码，与 HTTP 状态码无关。
     * 取值请参考 {@link ResultCode} 枚举定义：
     * <ul>
     *   <li>200 — 操作成功</li>
     *   <li>400 — 操作失败（通用业务错误）</li>
     *   <li>401 — 未认证（需要登录）</li>
     *   <li>403 — 权限不足（非管理员访问管理接口）</li>
     *   <li>404 — 资源不存在</li>
     *   <li>500 — 服务器内部错误</li>
     * </ul>
     */
    private int code;

    /**
     * 业务提示消息，供前端展示给用户。
     * 例如 "操作成功"、"用户名或密码错误"、"音乐上传成功" 等，
     * 可根据业务场景定制。
     */
    private String message;

    /**
     * 实际响应数据，泛型类型。
     * 当请求成功时，data 包含业务数据（如音乐列表、用户信息等）；
     * 当请求失败时，data 通常为 null（但也可以根据需要在 fail 中附带数据）。
     */
    private T data;

    /** 无参构造器，用于反序列化（Jackson） */
    public Result() {}

    /**
     * 全参构造器，一次设置 code、message 和 data。
     * 通常不直接调用，而是使用静态工厂方法。
     *
     * @param code    状态码
     * @param message 提示消息
     * @param data    响应数据
     */
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 操作成功，并返回数据体。
     *
     * <p>用于查询类接口，例如获取音乐列表、获取用户信息等。
     * code 固定为 {@link ResultCode#SUCCESS}（200），
     * message 固定为 "操作成功"。</p>
     *
     * @param data 要返回的业务数据
     * @param <T>  数据类型
     * @return 包含数据的成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 操作成功，不返回数据体（data = null）。
     *
     * <p>用于执行类接口，例如删除音乐、取消点赞等不需要返回数据的操作。
     * 前端通过 code 判断是否成功，message 展示成功提示。</p>
     *
     * @param <T> 数据类型（通常为 Void 或 Object）
     * @return 无数据的成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 操作失败，携带自定义错误消息。
     *
     * <p>code 固定为 {@link ResultCode#FAIL}（400），
     * 用于业务校验失败的情况，如参数不合法、重复操作、资源已被删除等。</p>
     *
     * @param message 自定义的错误描述，前端通常会将其作为提示文案展示给用户
     * @param <T>     数据类型（data 固定为 null）
     * @return 带有自定义消息的失败响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /**
     * 操作失败，使用预定义的 {@link ResultCode} 枚举作为状态码和消息。
     *
     * <p>适用于已经定义好标准错误码的场景，如：</p>
     * <pre>{@code
     * return Result.fail(ResultCode.UNAUTHORIZED);  // {"code":401, "message":"请先登录"}
     * return Result.fail(ResultCode.FORBIDDEN);     // {"code":403, "message":"权限不足"}
     * }</pre>
     *
     * @param code 预定义的响应状态码枚举
     * @param <T>  数据类型（data 固定为 null）
     * @return 带有预定义状态码的失败响应
     */
    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    /**
     * 操作失败，完全自定义状态码和消息。
     *
     * <p>仅在 {@link #fail(ResultCode)} 和 {@link #fail(String)} 无法满足需求时使用，
     * 例如需要使用 400 以外的可扩展状态码。</p>
     *
     * @param code    自定义状态码
     * @param message 自定义错误消息
     * @param <T>     数据类型（data 固定为 null）
     * @return 完全自定义的失败响应
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}