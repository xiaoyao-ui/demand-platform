package com.demand.common;

import lombok.Data;

/**
 * 返回结果
 * @param <T>
 */
@Data
public class Result<T> {
    //状态码
    private Integer code;
    //提示消息
    private String message;
    //数据集
    private T data;

    /**
     * 成功返回 无返回数据
     * @return
     * @param <T>
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }

    /**
     * 成功返回 有返回数据
     * @param data
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 异常返回
     * @param message
     * @return
     * @param <T>
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.ERROR.getCode());
        result.setMessage(message);
        return result;
    }

    /**
     * 异常返回 手动设置状态码
     * @param code
     * @param message
     * @return
     * @param <T>
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}