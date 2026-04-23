package com.demand.config;

import com.demand.module.user.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 基于 AOP 的细粒度权限校验拦截器
 * <p>
 * 该类通过切面拦截所有带有 @RequirePermission 注解的 Controller 方法，
 * 在方法执行前进行身份鉴权。支持两种校验模式：
 * 1. 角色校验：验证当前用户是否拥有指定的角色（如管理员、项目经理）
 * 2. 资源归属校验：验证当前用户是否是操作资源的所有者（或管理员）
 * </p>
 * <p>
 * 设计优势：
 * - 非侵入式：业务代码只需添加注解，无需编写重复的 if-else 鉴权逻辑
 * - 灵活配置：通过注解参数动态指定所需角色和资源 ID 字段名
 * - 统一拦截：集中处理越权访问，抛出 403 Forbidden 异常
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionInterceptor {

    private final PermissionService permissionService;

    /**
     * 前置通知：在执行目标方法前进行权限校验
     * <p>
     * 拦截规则：匹配 @RequirePermission 注解
     * 执行顺序：
     * 1. 解析注解配置（所需角色、是否校验所有者、资源 ID 参数名）
     * 2. 若配置了角色要求，调用 checkRole 进行角色匹配
     * 3. 若开启了所有者校验，提取资源 ID 并验证归属权
     * 4. 全部通过则放行，否则抛出 BusinessException(403)
     * </p>
     *
     * @param joinPoint 连接点，用于获取方法信息和参数
     */
    @Before("@annotation(com.demand.config.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);

        if (requirePermission == null) {
            return;
        }

        // 1. 获取注解配置的权限规则
        int[] roles = requirePermission.roles();
        boolean requireOwner = requirePermission.requireOwner();
        String resourceIdParam = requirePermission.resourceIdParam();

        // 2. 执行角色校验（如果有配置）
        if (roles.length > 0) {
            permissionService.checkRole(roles);
        }

        // 3. 执行资源归属校验（如果开启）
        if (requireOwner) {
            Long resourceId = extractResourceId(joinPoint, resourceIdParam);
            if (resourceId != null) {
                permissionService.checkOwnerOrAdmin(resourceId);
            }
        }

        log.debug("权限校验通过: method={}, roles={}, requireOwner={}",
                method.getName(), Arrays.toString(roles), requireOwner);
    }

    /**
     * 从 HTTP 请求参数中动态提取资源 ID
     * <p>
     * 根据注解中指定的参数名（如 "id", "demandId"），
     * 从 URL 查询字符串或表单数据中获取对应的值。
     * </p>
     *
     * @param joinPoint  连接点
     * @param paramName  资源 ID 在请求中的参数名
     * @return 解析后的资源 ID，若不存在或格式错误则返回 null
     */
    private Long extractResourceId(JoinPoint joinPoint, String paramName) {
        try {
            // 获取当前请求上下文
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            String idValue = request.getParameter(paramName);

            if (idValue == null || idValue.isEmpty()) {
                return null;
            }

            return Long.parseLong(idValue);
        } catch (NumberFormatException e) {
            log.warn("无法解析资源 ID: paramName={}, error={}", paramName, e.getMessage());
            return null;
        }
    }
}
