package com.demand.common;

import lombok.Getter;

/**
 * 项目权限枚举
 */
@Getter
public enum ProjectRoleEnum {
    OWNER("OWNER", "项目负责人", 1),
    MANAGER("MANAGER", "项目经理", 2),
    PRODUCT_MANAGER("PRODUCT_MANAGER", "产品经理", 3),
    DEVELOPER("DEVELOPER", "开发工程师", 4),
    TESTER("TESTER", "测试工程师", 5),
    VIEWER("VIEWER", "观察者", 6);

    private final String code;
    private final String name;
    private final Integer level;

    ProjectRoleEnum(String code, String name, Integer level) {
        this.code = code;
        this.name = name;
        this.level = level;
    }

    public static ProjectRoleEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return VIEWER;
        }
        for (ProjectRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return VIEWER;
    }
}
