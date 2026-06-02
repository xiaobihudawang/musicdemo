package org.example.musicdemo.common;
import lombok.Data;

/**
 * 统一 API 返回格式，所有接口返回值都包装为此对象。
 * JSON 结构：{"code": 200, "message": "操作成功", "data": {...}}
 *
 * @param <T> data 字段的数据类型
 * @see org.example.musicdemo.common.ResultCode
 */
@Data
public class Result<T> {

    /** 业务状态码，参考 ResultCode 枚举 */
    private int code;

    /** 提示消息，供前端展示 */
    private String message;

    /** 响应数据，成功时有值，失败时为 null */
    private T data;

    /** 无参构造器，用于 Jackson 反序列化 */
    public Result() {}

    /** 全参构造器 */
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功，返回数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功，不返回数据 */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 失败，自定义错误消息（code = 400） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /** 失败，使用预定义的 ResultCode */
    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    /** 失败，完全自定义状态码和消息 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}