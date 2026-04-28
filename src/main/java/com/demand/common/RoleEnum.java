package com.demand.common;

import lombok.Getter;

@Getter
public enum RoleEnum {
    SUPER_ADMIN(1, "超级管理员", "SUPER_ADMIN"),
    
    USER(2, "普通用户", "USER"),
    
    PRODUCT_MANAGER(3, "产品经理", "PRODUCT_MANAGER"),
    
    PROJECT_MANAGER(4, "项目经理", "PROJECT_MANAGER"),
    
    DEVELOPER(5, "开发工程师", "DEVELOPER"),
    
    IMPLEMENTER(6, "实施/测试", "IMPLEMENTER"),

    GUEST(7, "只读用户", "GUEST");

    private final Integer code;
    private final String description;
    private final String roleKey;

    RoleEnum(Integer code, String description, String roleKey) {
        this.code = code;
        this.description = description;
        this.roleKey = roleKey;
    }

    public static RoleEnum fromCode(Integer code) {
        if (code == null) {
            return GUEST;
        }
        for (RoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return GUEST;
    }

    public static RoleEnum fromRoleKey(String roleKey) {
        if (roleKey == null || roleKey.isEmpty()) {
            return GUEST;
        }
        for (RoleEnum role : values()) {
            if (role.getRoleKey().equals(roleKey)) {
                return role;
            }
        }
        return GUEST;
    }
}
