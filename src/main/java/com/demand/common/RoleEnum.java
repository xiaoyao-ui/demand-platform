package com.demand.common;

import lombok.Getter;

/**
 * 角色枚举
 */
@Getter
public enum RoleEnum {
    /**
     * 只读用户 - 只能查看，不能操作
     */
    READ_ONLY(0, "只读用户"),
    
    /**
     * 普通用户
     */
    USER(1, "普通用户"),
    
    /**
     * 管理员
     */
    ADMIN(2, "管理员"),
    
    /**
     * 项目经理 - 可以审批需求
     */
    PROJECT_MANAGER(3, "项目经理");

    private final Integer code;
    private final String description;

    RoleEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RoleEnum fromCode(Integer code) {
        if (code == null) {
            return READ_ONLY;
        }
        for (RoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return READ_ONLY;
    }
}
