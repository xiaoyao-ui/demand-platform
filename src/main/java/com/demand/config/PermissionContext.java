package com.demand.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 权限上下文 - 存储当前请求的用户身份信息
 * <p>
 * 该类使用 @RequestScope 注解，确保每个 HTTP 请求都有独立的实例，
 * 避免多线程环境下用户信息串号（线程安全问题）。
 * </p>
 * <p>
 * 工作流程：
 * 1. JwtInterceptor 在请求拦截阶段解析 Token，将用户信息存入此类
 * 2. @RequirePermission 注解在方法执行前，从此类获取用户角色进行权限校验
 * 3. 请求结束后，该实例自动销毁，信息不残留
 * </p>
 * <p>
 * 角色定义（role 字段）：
 * - 0: 只读用户（仅可查看，不可操作）
 * - 1: 普通用户（可创建/编辑自己的需求）
 * - 2: 管理员（拥有所有权限）
 * - 3: 项目经理（可审批、分配需求）
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Data
@Component
@RequestScope
public class PermissionContext {
    
    /**
     * 当前登录用户的 ID
     */
    private Long userId;
    
    /**
     * 当前登录用户的用户名
     */
    private String username;
    
    /**
     * 当前登录用户的角色代码（0-只读，1-普通，2-管理员，3-项目经理）
     */
    private Integer role;
    
    /**
     * 检查当前用户是否具有指定角色
     *
     * @param roleCode 目标角色代码
     * @return true 表示拥有该角色，false 表示没有
     */
    public boolean hasRole(int roleCode) {
        return this.role != null && this.role == roleCode;
    }
    
    /**
     * 检查当前用户是否为管理员
     * <p>
     * 管理员角色代码为 2，拥有系统的最高权限。
     * </p>
     *
     * @return true 表示是管理员，false 表示不是
     */
    public boolean isAdmin() {
        return hasRole(2);
    }
    
    /**
     * 检查当前用户是否为只读用户
     * <p>
     * 只读用户角色代码为 0，只能查看数据，不能进行增删改操作。
     * </p>
     *
     * @return true 表示是只读用户，false 表示不是
     */
    public boolean isReadOnly() {
        return this.role != null && this.role == 0;
    }
}
