package org.example.musicdemo.common;
import lombok.Data;
/**
 * 统一 API 返回格式
 * 所有 Controller 的返回值都包装成这个对象
 *
 * 返回 JSON 样例如下：
 * {"code": 200, "message": "操作成功", "data": {...}}
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result() {}

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 失败（自定义消息） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /** 失败（使用预定义状态码） */
    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    /** 失败（自定义消息和状态码） */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}