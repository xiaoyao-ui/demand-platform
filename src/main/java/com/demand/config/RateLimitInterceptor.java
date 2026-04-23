package com.demand.config;

import com.demand.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器
 * <p>
 * 基于 Redis + 滑动时间窗口算法实现接口访问频率限制。
 * 拦截所有带有 @RateLimit 注解的请求，防止恶意刷接口、暴力破解或系统过载。
 * </p>
 * <p>
 * 限流维度：基于 "IP + 接口路径" 进行统计，确保同一 IP 对同一接口的访问频率受控。
 * 核心特性：
 * 1. 高性能：使用 Redis INCR 原子操作，支持分布式环境
 * 2. 自动过期：利用 Redis TTL 机制，避免 Key 永久堆积
 * 3. 灵活配置：通过注解动态设置时间窗口和请求上限
 * </p>
 * <p>
 * 工作流程：
 * 1. 检查目标方法是否标注 @RateLimit 注解，无则放行
 * 2. 构建 Redis Key：{keyPrefix}:{URI}:{IP}
 * 3. 执行 INCR 操作，计数器 +1
 * 4. 若是首次请求（count=1），设置过期时间（timeWindow）
 * 5. 若计数器超过 maxRequests，抛出 429 异常拦截请求
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    /**
     * 请求预处理：执行限流校验
     * <p>
     * 该方法会在请求到达 Controller 之前执行。
     * 若触发限流规则，直接抛出 BusinessException 阻断请求；
     * 若未触发或无注解，则放行请求。
     * </p>
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  目标处理器（Controller 方法）
     * @return true 表示放行请求，false 表示拦截
     * @throws BusinessException 当请求频率超过限制时抛出 429 错误
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 仅对 Controller 方法级别的注解生效，静态资源等直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 2. 检查方法是否标注了限流注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 3. 获取客户端真实 IP 和请求路径
        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        
        // 4. 构建唯一的限流 Key（格式：rate_limit:/api/login:192.168.1.100）
        String key = rateLimit.keyPrefix() + ":" + uri + ":" + ip;

        // 5. 执行 Redis INCR 原子递增操作
        Long count = redisTemplate.opsForValue().increment(key);
        
        // 6. 若是该 Key 的第一次请求，设置过期时间（防止 Key 永久残留）
        if (count == 1) {
            redisTemplate.expire(key, rateLimit.timeWindow(), TimeUnit.SECONDS);
        }

        // 7. 判断是否超限
        if (count > rateLimit.maxRequests()) {
            log.warn("接口限流触发：ip={}, uri={}, count={}", ip, uri, count);
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }

        return true;
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 优先从反向代理请求头获取 IP，适配 Nginx 等代理场景。
     * 检查优先级：
     * 1. X-Forwarded-For（标准代理头）
     * 2. X-Real-IP（Nginx 自定义头）
     * 3. getRemoteAddr()（兜底方案，直连时使用）
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
