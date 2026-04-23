package com.demand.common;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "成功"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    DEMAND_NOT_FOUND(1001, "需求不存在"),
    DEMAND_STATUS_ERROR(1002, "需求状态不允许此操作"),
    DEMAND_OWNER_ERROR(1003, "只有需求创建者才能执行此操作"),

    VERIFY_CODE_EXPIRED(2001, "验证码已过期"),
    VERIFY_CODE_ERROR(2002, "验证码错误"),
    VERIFY_CODE_LIMIT(2003, "验证码验证失败次数过多，请重新获取"),
    VERIFY_CODE_SEND_LIMIT(2004, "验证码发送过于频繁"),

    PERMISSION_DENIED(3001, "权限不足"),
    ADMIN_REQUIRED(3002, "需要管理员权限"),
    READ_ONLY_USER(3003, "只读用户无权执行此操作"),

    USER_NOT_FOUND(4001, "用户不存在"),
    USER_DISABLED(4002, "账号已被禁用"),
    USER_EXISTS(4003, "用户已存在"),

    SYSTEM_ERROR(5000, "系统内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
