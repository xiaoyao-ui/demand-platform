package com.demand.config;

import com.demand.exception.BusinessException;
import com.demand.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 身份认证拦截器
 * <p>
 * 作为系统安全的第一道防线，拦截所有进入 Controller 的请求，验证用户身份。
 * 验证通过后，将用户信息（userId, username, role）注入到请求上下文，
 * 供后续业务逻辑和权限校验使用。
 * </p>
 * <p>
 * 工作流程：
 * 1. 从请求头获取 Authorization 字段，校验格式是否为 "Bearer {token}"
 * 2. 调用 JwtUtil 校验 Token 的合法性（签名是否正确、是否过期）
 * 3. 从 Token 中解析用户信息并存入 PermissionContext（线程隔离）
 * 4. 放行请求，进入业务 Controller
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final PermissionContext permissionContext;

    /**
     * 请求预处理：验证 JWT Token
     * <p>
     * 该方法会在请求到达 Controller 之前执行。
     * 如果验证失败，直接抛出 BusinessException 阻断请求；
     * 如果验证成功，将用户信息存入上下文并放行。
     * </p>
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  目标处理器（Controller 方法）
     * @return true 表示放行请求，false 表示拦截
     * @throws BusinessException 当 Token 缺失或无效时抛出 401 错误
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 放行 OPTIONS 预检请求（跨域场景下浏览器会自动发送）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取并校验 Authorization 请求头
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("请求未提供认证令牌: uri={}, method={}, ip={}", 
                    request.getRequestURI(), request.getMethod(), getClientIp(request));
            throw new BusinessException(401, "未提供认证令牌");
        }

        // 3. 去除 "Bearer " 前缀，验证 Token 有效性
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.warn("认证令牌无效或已过期: uri={}, method={}, ip={}", 
                    request.getRequestURI(), request.getMethod(), getClientIp(request));
            throw new BusinessException(401, "认证令牌无效或已过期");
        }

        // 4. 从 Token 中解析用户身份信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        Integer role = jwtUtil.getRoleFromToken(token);
        
        // 5. 将用户信息存入线程隔离的上下文（供 @RequirePermission 等组件使用）
        permissionContext.setUserId(userId);
        permissionContext.setUsername(username);
        permissionContext.setRole(role);
        
        // 6. 同时存入 Request 属性（兼容旧代码或特定场景）
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);
        
        // 7. 记录访问日志
        log.info("用户操作: userId={}, username={}, role={}, uri={}, method={}, ip={}", 
                userId, username, role, request.getRequestURI(), request.getMethod(), getClientIp(request));
        return true;
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 由于项目可能部署在 Nginx 等反向代理之后，直接使用 request.getRemoteAddr()
     * 获取的可能是代理服务器的 IP。因此需要依次检查常见的代理请求头。
     * </p>
     * <p>
     * 检查优先级：
     * 1. X-Forwarded-For（标准代理头，Nginx 默认使用）
     * 2. Proxy-Client-IP（Apache 代理头）
     * 3. WL-Proxy-Client-IP（WebLogic 代理头）
     * 4. X-Real-IP（Nginx 自定义头）
     * 5. getRemoteAddr()（兜底方案，直连时使用）
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
