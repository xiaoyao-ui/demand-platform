package com.demand.module.project.service;

import com.demand.config.PermissionContext;
import com.demand.module.project.entity.ProjectMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 综合权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprehensivePermissionService {

    private final PermissionContext permissionContext;
    private final ProjectPermissionService projectPermissionService;

    /**
     * 综合判断用户是否有权限执行某项操作
     * <p>
     * 权限判断逻辑：系统权限 AND 项目权限
     * 1. 超级管理员拥有所有权限（ bypass 项目权限检查）
     * 2. 其他用户需要同时满足系统权限和项目权限
     * </p>
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param permission 权限标识
     * @return 是否有权限
     */
    public boolean hasComprehensivePermission(Long projectId, Long userId, String permission) {
        // 1. 超级管理员拥有所有权限
        if (permissionContext.isAdmin()) {
            log.debug("超级管理员拥有所有权限: userId={}, permission={}", userId, permission);
            return true;
        }

        // 2. 检查项目角色权限
        ProjectMember member = projectPermissionService.getProjectMember(projectId, userId);
        if (member == null) {
            log.debug("用户不是项目成员: userId={}, projectId={}", userId, projectId);
            return false;
        }

        boolean hasPermission = switch (permission) {
            case "manage_project" -> member.canManageProject();
            case "manage_iteration" -> member.canManageIteration();
            case "view_demand" -> member.canViewDemand();
            case "create_demand" -> member.canCreateDemand();
            case "edit_demand" -> member.canEditDemand();
            case "approve_demand" -> member.canApproveDemand();
            case "assign_demand" -> member.canAssignDemand();
            default -> {
                log.warn("未知的权限标识: {}", permission);
                yield false;
            }
        };

        log.debug("权限检查结果: userId={}, projectId={}, permission={}, result={}",
                userId, projectId, permission, hasPermission);
        return hasPermission;
    }

    /**
     * 检查用户是否有综合权限，如果没有则抛出异常
     */
    public void checkComprehensivePermission(Long projectId, Long userId, String permission, String message) {
        if (!hasComprehensivePermission(projectId, userId, permission)) {
            throw new com.demand.exception.BusinessException(message);
        }
    }
}
