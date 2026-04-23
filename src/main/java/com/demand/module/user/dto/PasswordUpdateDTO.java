package com.demand.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密码修改请求对象
 */
@Data
public class PasswordUpdateDTO {

    /**
     * 旧密码
     */
    @NotBlank(message = "当前密码不能为空")
    @Schema(description = "当前密码", example = "OldPass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    /**
     * 新密码
     * <p>
     * 规则：8-20 位，必须包含字母和数字
     * </p>
     */
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$", message = "新密码格式不正确（8-20位，必须包含字母和数字）")
    @Schema(description = "新密码", example = "NewPass456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
