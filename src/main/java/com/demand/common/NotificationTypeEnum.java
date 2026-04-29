package com.demand.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    SYSTEM(1, "系统通知"),
    COMMENT(2, "评论通知"),
    APPROVAL_REMINDER(3, "审批提醒"),
    ASSIGNMENT(4, "分配通知"),
    STATUS_CHANGE(5, "状态变更");

    private final Integer code;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static NotificationTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (NotificationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
