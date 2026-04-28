package com.demand.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

/**
 * 权限上下文：用于在请求链路中传递当前用户的身份信息
 */
@Data
@Component
public class PermissionContext {

    //存储用户上下文信息
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> rolesHolder = new ThreadLocal<>();

    /**
     * 设置用户ID
     */
    public void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    /**
     * 获取用户ID
     */
    public Long getUserId() {
        return userIdHolder.get();
    }

    /**
     * 设置用户名
     */
    public void setUsername(String username) {
        usernameHolder.set(username);
    }

    /**
     * 获取用户名
     */
    public String getUsername() {
        return usernameHolder.get();
    }

    /**
     * 设置角色列表
     */
    public void setRoles(List<String> roles) {
        rolesHolder.set(roles);
    }

    /**
     * 获取角色列表
     */
    public List<String> getRoles() {
        return rolesHolder.get() != null ? rolesHolder.get() : Collections.emptyList();
    }

    /**
     * 是否为超级管理员
     */
    public boolean isAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    /**
     * 是否为产品经理
     */
    public boolean isProductManager() {
        return hasRole("PRODUCT_MANAGER") || isAdmin();
    }

    /**
     * 是否为项目经理
     */
    public boolean isProjectManager() {
        return hasRole("PROJECT_MANAGER") || isAdmin();
    }

    /**
     * 是否为开发人员
     */
    public boolean isDeveloper() {
        return hasRole("DEVELOPER") || isAdmin();
    }

    /**
     * 是否为实施/测试人员
     */
    public boolean isImplementer() {
        return hasRole("IMPLEMENTER") || isAdmin();
    }

    /**
     * 是否为普通用户
     */
    public boolean isUser() {
        return hasRole("USER");
    }

    /**
     * 判断是否可以创建需求
     */
    public boolean canCreateDemand() {
        return isUser() || isProductManager() || isProjectManager() ||
               isDeveloper() || isImplementer() || isAdmin();
    }

    /**
     * 判断是否能审批需求
     */
    public boolean canApproveDemand() {
        return isProjectManager() || isAdmin();
    }

    /**
     * 判断是否能分配需求
     */
    public boolean canAssignDemand() {
        return isProjectManager() || isAdmin();
    }

    /**
     * 判断是否能开发需求
     */
    public boolean canDevelopDemand() {
        return isDeveloper() || isProjectManager() || isAdmin();
    }

    /**
     * 判断是否能测试需求
     */
    public boolean canTestDemand() {
        return isImplementer() || isProjectManager() || isAdmin();
    }

    /**
     * 判断是否可以查看所有需求
     */
    public boolean canViewAllDemands() {
        return isAdmin() || isProjectManager() || isProductManager() || isImplementer();
    }

    /**
     * 判断是否拥有指定角色
     */
    public boolean hasRole(String roleKey) {
        List<String> roles = rolesHolder.get();
        return roles != null && roles.contains(roleKey);
    }

    /**
     * 清空权限上下文
     */
    public void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        rolesHolder.remove();
    }
}
