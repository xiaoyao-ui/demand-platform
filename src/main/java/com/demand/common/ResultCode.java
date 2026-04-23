package com.demand.common;

import lombok.Getter;

/**
 * 状态码枚举
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    DEMAND_NOT_FOUND(1001, "需求不存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    COMMENT_NOT_FOUND(1003, "评论不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
