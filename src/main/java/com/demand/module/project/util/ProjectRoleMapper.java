package com.demand.module.project.util;

import com.demand.common.ProjectRoleEnum;
import java.util.Map;

/**
 * 系统角色到项目角色的映射工具类
 * <p>
 * 提供系统角色到默认项目角色的自动映射功能，
 * 在添加项目成员时如果没有指定项目角色，则根据系统角色自动分配。
 * </p>
 */
public class ProjectRoleMapper {

    /**
     * 系统角色到项目角色的映射关系
     */
    private static final Map<String, String> SYSTEM_TO_PROJECT_ROLE = Map.of(
            "SUPER_ADMIN", ProjectRoleEnum.OWNER.getCode(),
            "PROJECT_MANAGER", ProjectRoleEnum.MANAGER.getCode(),
            "PRODUCT_MANAGER", ProjectRoleEnum.PRODUCT_MANAGER.getCode(),
            "DEVELOPER", ProjectRoleEnum.DEVELOPER.getCode(),
            "IMPLEMENTER", ProjectRoleEnum.TESTER.getCode(),
            "USER", ProjectRoleEnum.VIEWER.getCode(),
            "GUEST", ProjectRoleEnum.VIEWER.getCode()
    );

    /**
     * 根据系统角色获取默认的项目角色
     *
     * @param systemRoleKey 系统角色标识（如：SUPER_ADMIN, DEVELOPER）
     * @return 对应的项目角色标识，如果未找到映射则返回 VIEWER
     */
    public static String getDefaultProjectRole(String systemRoleKey) {
        if (systemRoleKey == null || systemRoleKey.isEmpty()) {
            return ProjectRoleEnum.VIEWER.getCode();
        }
        return SYSTEM_TO_PROJECT_ROLE.getOrDefault(systemRoleKey, ProjectRoleEnum.VIEWER.getCode());
    }

    /**
     * 根据用户的系统角色列表获取最合适的项目角色
     * <p>
     * 如果用户有多个系统角色，选择权限最高的那个进行映射
     * </p>
     *
     * @param systemRoleKeys 用户的系统角色列表
     * @return 推荐的项目角色标识
     */
    public static String recommendProjectRole(java.util.List<String> systemRoleKeys) {
        if (systemRoleKeys == null || systemRoleKeys.isEmpty()) {
            return ProjectRoleEnum.VIEWER.getCode();
        }

        // 优先级排序：SUPER_ADMIN > PROJECT_MANAGER > PRODUCT_MANAGER > DEVELOPER > IMPLEMENTER > USER > GUEST
        for (String highPriorityRole : new String[]{"SUPER_ADMIN", "PROJECT_MANAGER", "PRODUCT_MANAGER",
                "DEVELOPER", "IMPLEMENTER", "USER", "GUEST"}) {
            if (systemRoleKeys.contains(highPriorityRole)) {
                return getDefaultProjectRole(highPriorityRole);
            }
        }

        return ProjectRoleEnum.VIEWER.getCode();
    }
}
