package com.demand.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 用户信息更新请求对象
 * <p>
 * 所有字段均为可选，仅更新传入的字段。
 * </p>
 */
@Data
public class UserUpdateDTO {

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    /**
     * 邮箱地址
     */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;

    /**
     * 手机号（修改时需要验证码验证）
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 邮箱验证码（修改邮箱时必填）
     */
    @Schema(description = "邮箱验证码", example = "123456")
    private String emailCode;

    /**
     * 手机验证码（修改手机号时必填）
     */
    @Schema(description = "手机验证码", example = "123456")
    private String phoneCode;

    /**
     * 角色编码（仅管理员可修改）
     * <ul>
     *   <li>0 - 只读用户</li>
     *   <li>1 - 普通用户</li>
     *   <li>2 - 管理员</li>
     *   <li>3 - 项目经理</li>
     * </ul>
     */
    @Schema(description = "角色编码", example = "1")
    private Integer role;
}