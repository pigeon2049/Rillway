package com.wegongdu.rillway.example.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "统一 API 响应结果")
public class CommonResult<T> implements Serializable {

    @Schema(description = "状态码 (200 为成功)", example = "200")
    private int code;

    @Schema(description = "响应提示信息", example = "操作成功")
    private String message;

    @Schema(description = "响应业务数据")
    private T data;

    public CommonResult() {}

    public CommonResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "success", data);
    }

    public static <T> CommonResult<T> success(String message, T data) {
        return new CommonResult<>(200, message, data);
    }

    public static <T> CommonResult<T> error(int code, String message) {
        return new CommonResult<>(code, message, null);
    }

    public static <T> CommonResult<T> error(String message) {
        return new CommonResult<>(500, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
