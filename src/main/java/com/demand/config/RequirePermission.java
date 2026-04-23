package com.demand.config;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * <p>
 * 用于标记需要权限控制的接口或类，配合 {@link PermissionInterceptor} 使用。
 * 支持基于角色（Role）和资源所有权（Owner）的双重校验机制。
 * </p>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>限制特定角色访问：{@code @RequirePermission(roles = {1, 2})}</li>
 *   <li>要求资源所有者：{@code @RequirePermission(requireOwner = true)}</li>
 *   <li>组合校验：同时验证角色和所有权</li>
 * </ul>
 * 
 * <h3>示例：</h3>
 * <pre>{@code
 * @RequirePermission(roles = {1}, requireOwner = true, resourceIdParam = "id")
 * @PutMapping("/demands/{id}")
 * public Result updateDemand(@PathVariable Long id) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的角色列表
     * <p>
     * 角色值对应 {@link com.demand.common.RoleEnum} 中的编码。
     * 为空表示不限制角色，仅校验所有权或其他条件。
     * </p>
     *
     * @return 允许访问的角色编码数组
     */
    int[] roles() default {};

    /**
     * 是否需要是资源所有者
     * <p>
     * 开启后，拦截器会从请求参数中提取资源ID（通过 {@link #resourceIdParam}），
     * 并查询数据库验证当前用户是否为该资源的创建者。
     * </p>
     *
     * @return true 表示要求资源所有者，false 表示不校验所有权
     */
    boolean requireOwner() default false;

    /**
     * 资源ID参数名
     * <p>
     * 当 {@link #requireOwner()} 为 true 时，拦截器会从请求参数中提取此名称的值作为资源ID。
     * 支持路径变量（@PathVariable）和请求参数（@RequestParam）。
     * </p>
     *
     * @return 资源ID的参数名，默认为 "id"
     */
    String resourceIdParam() default "id";
}
