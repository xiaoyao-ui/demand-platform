package com.demand.module.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码通知消息
 * <p>
 * 用于通过 WebSocket 实时推送验证码到前端，提升用户体验。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeNotification {

    /**
     * 消息类型（固定为 "VERIFICATION_CODE"）
     */
    private String type;

    /**
     * 验证码
     */
    private String code;

    /**
     * 验证码类型（"phone" 或 "email"）
     */
    private String codeType;

    /**
     * 目标（手机号或邮箱，脱敏显示）
     */
    private String target;

    /**
     * 有效期（秒）
     */
    private Integer expiration;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;
}
