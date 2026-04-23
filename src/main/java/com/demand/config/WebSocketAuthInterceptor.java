package com.demand.config;

import com.demand.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

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
 *   <li>验证通过后，将 userId 存入 {@code attributes}，供后续 {@link NotificationWebSocketHandler} 使用</li>
 *   <li>验证失败则拒绝握手，返回 HTTP 401 错误</li>
 * </ol>
 * 
 * <h3>Token 获取优先级：</h3>
 * <ol>
 *   <li>URL 参数：{@code ws://localhost:8080/ws?token=xxx}</li>
 *   <li>Authorization Header：{@code Authorization: Bearer xxx}</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    /**
     * JWT 工具类，用于解析 Token 并提取用户信息
     */
    private final JwtUtil jwtUtil;

    /**
     * 握手前执行的身份验证逻辑
     * <p>
     * 核心步骤：
     * 1. 从 HTTP 请求中提取 Token（支持 URL 参数和 Header 两种方式）
     * 2. 调用 {@link JwtUtil#getUserIdFromToken} 验证 Token 有效性
     * 3. 将 userId 存入 {@code attributes} Map，后续可通过 {@link org.springframework.web.socket.WebSocketSession#getAttributes()} 获取
     * </p>
     * 
     * <p>
     * <b>为什么需要 attributes？</b><br>
     * WebSocket 建立后是独立于 HTTP 的长连接，无法再次获取请求头。
     * 通过在握手阶段将 userId 存入 attributes，可以在后续的 {@link NotificationWebSocketHandler} 中识别用户身份。
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

                // 1. 从请求参数或 Header 获取 Token
                String token = httpServletRequest.getParameter("token");
                if (token == null || token.isEmpty()) {
                    String authHeader = httpServletRequest.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    }
                }

                // 2. 验证 Token 并提取用户 ID
                if (token != null && !token.isEmpty()) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    if (userId != null) {
                        // 3. 将 userId 存入 attributes，供 WebSocketHandler 使用
                        attributes.put("userId", userId);
                        log.info("WebSocket 握手成功，用户ID: {}", userId);
                        return true;
                    }
                }
            }

            log.warn("WebSocket 握手失败：Token 无效");
            return false;
        } catch (Exception e) {
            log.error("WebSocket 握手异常", e);
            return false;
        }
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
        // 不需要额外处理
    }
}
