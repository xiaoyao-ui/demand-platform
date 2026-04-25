package com.demand.config;

import com.demand.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 握手拦截器 - JWT 身份验证
 * <p>
 * 在 WebSocket 握手阶段（HTTP 升级为 WS 协议之前）进行身份验证，
 * 确保只有合法用户才能建立长连接。通过 {@link HandshakeInterceptor} 接口实现。
 * </p>
 * 
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>前端发起 WebSocket 连接请求，携带 Token（URL 参数或 Authorization Header）</li>
 *   <li>{@link #beforeHandshake} 方法提取并验证 Token</li>
 *   <li>验证通过后，将 userId 存入 {@code attributes}，供后续 {@link WebSocketHandlerImpl} 使用</li>
 *   <li>验证失败则拒绝握手，返回 HTTP 401 错误</li>
 * </ol>
 * 
 * <h3>Token 获取优先级：</h3>
 * <ol>
 *   <li>URL 参数：{@code ws://localhost:8080/ws/notification?token=xxx}</li>
 *   <li>Authorization Header：{@code Authorization: Bearer xxx}</li>
 * </ol>
 * 
 * <h3>为什么需要这个拦截器？</h3>
 * <p>
 * WebSocket 是长连接协议，一旦建立就无法再次进行身份验证。
 * 因此必须在握手阶段（此时仍是 HTTP 协议）完成鉴权，并将用户信息传递给后续的 WebSocket 会话。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * JWT 工具类，用于解析 Token 并提取用户信息
     */
    private final JwtUtil jwtUtil;

    /**
     * Redis 模板，用于 IP 限流
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 是否允许匿名 WebSocket 连接
     * <p>
     * 开发环境：true（允许未登录用户接收验证码）<br>
     * 生产环境：false（必须登录才能建立连接）
     * </p>
     */
    @Value("${websocket.anonymous.enabled:false}")
    private Boolean anonymousEnabled;

    /**
     * 握手前执行的身份验证逻辑
     * <p>
     * 核心步骤：
     * 1. 从 HTTP 请求中提取 Token（支持 URL 参数和 Header 两种方式）
     * 2. 调用 {@link JwtUtil#getUserIdFromToken} 验证 Token 有效性
     * 3. 将 userId 存入 {@code attributes} Map，供后续 {@link WebSocketHandlerImpl} 使用
     * </p>
     * 
     * <p>
     * <b>为什么需要 attributes？</b><br>
     * WebSocket 建立后是独立于 HTTP 的长连接，无法再次获取请求头。
     * 通过在握手阶段将 userId 存入 attributes，可以在后续的 {@link WebSocketHandlerImpl} 中识别用户身份。
     * </p>
     *
     * @param request     WebSocket 握手请求（包含 HTTP 信息）
     * @param response    WebSocket 握手响应
     * @param wsHandler   目标 WebSocket 处理器
     * @param attributes  属性集合，用于在握手阶段和 WebSocket 会话之间传递数据
     * @return true 允许握手，false 拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

                // 获取客户端 IP
                String clientIp = getClientIp(httpServletRequest);

                // 1. 从请求参数或 Header 获取 Token
                String token = httpServletRequest.getParameter("token");
                if (token == null || token.isEmpty()) {
                    token = httpServletRequest.getHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                }

                // 2. 验证 Token 并提取用户 ID
                if (token != null && !token.isEmpty()) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    if (userId != null) {
                        // 3. 将 userId 存入 attributes，供 WebSocketHandler 使用
                        attributes.put("userId", userId);
                        log.info("WebSocket 握手成功，用户ID: {}, IP: {}", userId, clientIp);
                        return true;
                    }
                }

                // 4. 无 Token 时，检查是否允许匿名连接
                if (!anonymousEnabled) {
                    log.warn("WebSocket 握手失败：匿名连接已禁用, IP: {}", clientIp);
                    return false;
                }

                // 5. IP 限流：同一 IP 最多建立 5 个匿名连接（10分钟内）
                String anonymousKey = "ws:anonymous:ip:" + clientIp;
                Long count = redisTemplate.opsForValue().increment(anonymousKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(anonymousKey, 10, TimeUnit.MINUTES);
                }

                if (count != null && count > 5) {
                    log.warn("WebSocket 握手失败：IP {} 匿名连接数超限 ({})", clientIp, count);
                    return false;
                }

                // 6. 允许匿名连接（用于接收验证码等）
                String anonymousId = "anonymous_" + System.currentTimeMillis();
                attributes.put("userId", -1L); // 使用 -1 表示匿名用户
                attributes.put("anonymousId", anonymousId);
                log.info("WebSocket 握手成功（匿名用户）: {}, IP: {}", anonymousId, clientIp);
                return true;
            }

            log.warn("WebSocket 握手失败：无效的请求类型");
            return false;
        } catch (Exception e) {
            log.error("WebSocket 握手异常", e);
            return false;
        }
    }

    /**
     * 获取客户端真实 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 握手后执行的回调（可选）
     * <p>
     * 通常用于资源清理或日志记录。本项目无需额外处理，因此留空。
     * </p>
     *
     * @param request     WebSocket 握手请求
     * @param response    WebSocket 握手响应
     * @param wsHandler   目标 WebSocket 处理器
     * @param exception   握手过程中发生的异常（如果有）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后不需要额外处理
    }
}
