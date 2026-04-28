package com.demand.module.user.service;

import com.demand.config.PermissionContext;
import com.demand.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 权限验证服务
 * <p>
 * 提供基于角色（RBAC）和资源所有权的权限校验功能。
 * 从 {@link PermissionContext} 中获取当前用户信息，执行业务层的权限判断。
 * </p>
 * 
 * <h3>核心方法：</h3>
 * <ul>
 *   <li>{@link #checkRole}：验证用户是否具有指定角色</li>
 *   <li>{@link #checkAdmin}：验证是否为管理员</li>
 *   <li>{@link #checkOwnerOrAdmin}：验证是否为资源所有者或管理员</li>
 *   <li>{@link #getCurrentUserId}：获取当前登录用户 ID</li>
 * </ul>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>Service 层方法调用前进行权限预检</li>
 *   <li>动态菜单生成时过滤无权限的菜单项</li>
 *   <li>数据查询时根据角色过滤可见范围</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    /**
     * 权限上下文，存储当前请求的用户信息
     */
    private final PermissionContext permissionContext;

    /**
     * 获取当前登录用户的角色列表
     */
    public List<String> getCurrentUserRoles() {
        return permissionContext.getRoles();
    }

    /**
     * 判断当前用户是否拥有指定角色
     */
    public boolean hasRole(String roleKey) {
        return permissionContext.hasRole(roleKey);
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUsername() {
        return permissionContext.getUsername();
    }
    
    /**
     * 检查用户是否具有指定角色之一
     * <p>
     * 支持传入多个角色代码，只要用户拥有其中之一即可通过验证。
     * </p>
     *
     * @param requiredRoles 允许的角色代码列表
     * @throws BusinessException 当用户角色不在允许列表中时抛出 403 错误
     */
    public void checkRole(int... requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            throw new IllegalArgumentException("至少需要指定一个角色进行权限验证");
        }

        List<String> userRoles = permissionContext.getRoles();

        if (userRoles == null || userRoles.isEmpty()) {
            log.warn("权限验证失败: userId={}, 原因=用户角色未定义", permissionContext.getUserId());
            throw new BusinessException(403, "用户角色未定义");
        }

        // 检查用户是否拥有任何一个所需角色
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(userRoles::contains);

        if (hasRequiredRole) {
            log.debug("权限验证通过: userId={}, roles={}", permissionContext.getUserId(), userRoles);
            return;
        }

        log.warn("权限验证失败: userId={}, userRoles={}, requiredRoles={}",
                permissionContext.getUserId(), userRoles, Arrays.toString(requiredRoles));
        throw new BusinessException(403, "没有权限执行此操作，所需角色: " + Arrays.toString(requiredRoles));
    }

    /**
     * 检查是否为管理员
     */
    public void checkAdmin() {
        if (!permissionContext.isAdmin()) {
            log.warn("非超级管理员尝试执行管理操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "需要超级管理员权限");
        }
    }

    /**
     * 检查是否为项目经理
     */
    public void checkProjectManager() {
        if (!permissionContext.isProjectManager()) {
            log.warn("非项目经理尝试执行项目管理操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "需要项目经理权限");
        }
    }

    /**
     * 检查是否为产品经理
     */
    public void checkProductManager() {
        if (!permissionContext.isProductManager()) {
            log.warn("非产品经理尝试执行产品管理操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "需要产品经理权限");
        }
    }

    /**
     * 检查是否为开发工程师
     */
    public void checkDeveloper() {
        if (!permissionContext.isDeveloper()) {
            log.warn("非开发工程师尝试执行开发操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "需要开发工程师权限");
        }
    }

    /**
     * 检查是否为实施/测试人员
     */
    public void checkImplementer() {
        if (!permissionContext.isImplementer()) {
            log.warn("非实施/测试人员尝试执行测试操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "需要实施/测试权限");
        }
    }

    /**
     * 检查当前用户是否具有创建需求权限
     */
    public void checkCanCreateDemand() {
        if (!permissionContext.canCreateDemand()) {
            log.warn("无权限创建需求: userId={}, roles={}", 
                    permissionContext.getUserId(), permissionContext.getRoles());
            throw new BusinessException(403, "没有权限创建需求");
        }
    }

    /**
     * 检查当前用户是否具有审批需求权限
     */
    public void checkCanApproveDemand() {
        if (!permissionContext.canApproveDemand()) {
            log.warn("无权限审批需求: userId={}, roles={}", 
                    permissionContext.getUserId(), permissionContext.getRoles());
            throw new BusinessException(403, "没有权限审批需求，需要项目经理或超级管理员权限");
        }
    }

    /**
     * 检查当前用户是否具有分配需求权限
     */
    public void checkCanAssignDemand() {
        if (!permissionContext.canAssignDemand()) {
            log.warn("无权限分配需求: userId={}, roles={}", 
                    permissionContext.getUserId(), permissionContext.getRoles());
            throw new BusinessException(403, "没有权限分配需求，需要项目经理或超级管理员权限");
        }
    }

    /**
     * 检查是否为资源所有者或管理员
     * <p>
     * 用于保护用户私有资源（如需求、评论），确保只有创建者或管理员可以修改/删除。
     * </p>
     * 
     * <p>
     * <b>使用示例</b>：
     * <pre>{@code
     * // 只有需求创建者或管理员可以删除
     * Demand demand = demandMapper.findById(id);
     * checkOwnerOrAdmin(demand.getProposerId());
     * }</pre>
     * </p>
     *
     * @param resourceOwnerId 资源所有者 ID
     * @throws BusinessException 当用户既不是所有者也不是管理员时抛出 403 错误
     */
    public void checkOwnerOrAdmin(Long resourceOwnerId) {
        if (resourceOwnerId == null) {
            throw new BusinessException(400, "资源所有者ID不能为空");
        }
        
        Long currentUserId = permissionContext.getUserId();
        
        if (currentUserId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        // 管理员直接放行
        if (permissionContext.isAdmin()) {
            log.debug("管理员访问资源: userId={}, resourceId={}", currentUserId, resourceOwnerId);
            return;
        }
        
        // 资源所有者放行
        if (currentUserId.equals(resourceOwnerId)) {
            log.debug("资源所有者访问: userId={}, resourceId={}", currentUserId, resourceOwnerId);
            return;
        }
        
        log.warn("无权访问资源: userId={}, resourceId={}, ownerId={}", 
                currentUserId, resourceOwnerId, resourceOwnerId);
        throw new BusinessException(403, "无权访问此资源");
    }
    
    /**
     * 获取当前登录用户 ID
     * <p>
     * 从 {@link PermissionContext} 中提取 userId，如果未登录则抛出异常。
     * 适用于需要记录操作人或关联当前用户的场景。
     * </p>
     *
     * @return 当前用户 ID
     * @throws BusinessException 当用户未登录时抛出 401 错误
     */
    public Long getCurrentUserId() {
        Long userId = permissionContext.getUserId();
        if (userId == null) {
            log.warn("用户未登录");
            throw new BusinessException(401, "用户未登录");
        }
        return userId;
    }
}
