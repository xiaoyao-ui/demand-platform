package com.demand.module.user.service;

import com.demand.config.PermissionContext;
import com.demand.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * 权限上下文，存储当前请求的用户信息（userId、role）
     */
    private final PermissionContext permissionContext;
    
    /**
     * 检查用户是否具有指定角色之一
     * <p>
     * 支持传入多个角色代码，只要用户拥有其中之一即可通过验证。
     * </p>
     * 
     * <p>
     * <b>使用示例</b>：
     * <pre>{@code
     * // 只允许管理员和项目经理访问
     * checkRole(2, 3);
     * }</pre>
     * </p>
     *
     * @param requiredRoles 允许的角色代码列表
     * @throws BusinessException 当用户角色不在允许列表中时抛出 403 错误
     */
    public void checkRole(int... requiredRoles) {
        Integer userRole = permissionContext.getRole();
        
        if (userRole == null) {
            throw new BusinessException(403, "用户角色未定义");
        }
        
        for (int requiredRole : requiredRoles) {
            if (userRole == requiredRole) {
                log.debug("权限验证通过: userId={}, role={}", permissionContext.getUserId(), requiredRole);
                return;
            }
        }
        
        log.warn("权限验证失败: userId={}, userRole={}, requiredRoles={}", 
                permissionContext.getUserId(), userRole, requiredRoles);
        throw new BusinessException(403, "没有权限执行此操作");
    }
    
    /**
     * 检查是否为管理员
     * <p>
     * 快捷方法，等价于 {@code checkRole(2)}。
     * 用于需要管理员权限的操作，如用户管理、系统配置等。
     * </p>
     *
     * @throws BusinessException 当用户不是管理员时抛出 403 错误
     */
    public void checkAdmin() {
        if (!permissionContext.isAdmin()) {
            log.warn("非管理员尝试执行管理操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "需要管理员权限");
        }
    }
    
    /**
     * 检查是否不是只读用户
     * <p>
     * 用于写操作前的权限预检，防止只读用户（role=0）执行增删改操作。
     * </p>
     *
     * @throws BusinessException 当用户是只读角色时抛出 403 错误
     */
    public void checkNotReadOnly() {
        Integer userRole = permissionContext.getRole();
        if (userRole != null && userRole == 0) {
            log.warn("只读用户尝试执行写操作: userId={}", permissionContext.getUserId());
            throw new BusinessException(403, "只读用户无权执行此操作");
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

    /**
     * 获取当前登录用户角色
     * <p>
     * 从 {@link PermissionContext} 中提取角色编码，用于动态权限判断。
     * </p>
     *
     * @return 当前用户角色编码
     * @throws BusinessException 当用户未登录时抛出 401 错误
     */
    public Integer getCurrentUserRole() {
        Integer role = permissionContext.getRole();
        if (role == null) {
            log.warn("用户未登录");
            throw new BusinessException(401, "用户未登录");
        }
        return role;
    }
    
    /**
     * 判断当前用户是否为只读角色
     *
     * @return true 表示只读用户，false 表示其他角色
     */
    public boolean isReadOnly() {
        Integer role = permissionContext.getRole();
        return role != null && role == 0;
    }
}
