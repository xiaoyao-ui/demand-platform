package com.demand.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求对象
 * <p>
 * 用于接收前端提交的注册表单数据，包含用户名、密码、邮箱、验证码等字段。
 * 通过 Jakarta Validation 注解实现自动参数校验。
 * </p>
 */
@Data
@Schema(description = "用户注册请求对象")
public class UserRegisterDTO {

    /**
     * 用户名
     * <p>
     * 规则：4-20 位字母、数字或下划线
     * </p>
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    /**
     * 密码
     * <p>
     * 规则：8-20 位，必须包含字母和数字
     * </p>
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * 邮箱地址
     */
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 邮箱验证码
     */
    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度为6位")
    @Schema(description = "邮箱验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String emailCode;

    /**
     * 手机号（可选）
     */
    @Size(max = 20, message = "手机号长度不能超过20")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 真实姓名（可选）
     */
    @Size(max = 50, message = "真实姓名长度不能超过50")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;
}
