package com.demand.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 全局配置类
 * <p>
 * 负责注册 WebSocket 端点和配置握手拦截器，实现服务端向客户端的实时消息推送。
 * 通过 {@link EnableWebSocket} 注解启用 Spring WebSocket 支持。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>端点注册</b>：{@code /ws/notification} - 用于推送系统通知、审批提醒等实时消息</li>
 *   <li><b>身份验证</b>：通过 {@link WebSocketHandshakeInterceptor} 在握手阶段验证 JWT Token</li>
 *   <li><b>跨域支持</b>：允许所有来源的连接（生产环境建议限制为前端域名）</li>
 *   <li><b>SockJS 降级</b>：当浏览器不支持 WebSocket 时，自动降级为轮询或 SSE</li>
 * </ul>
 * 
 * <h3>前端连接示例：</h3>
 * <pre>{@code
 * // 方式1：原生 WebSocket
 * const ws = new WebSocket('ws://localhost:8080/ws/notification?token=xxx');
 * 
 * // 方式2：SockJS（推荐，兼容性更好）
 * const socket = new SockJS('http://localhost:8080/ws/notification');
 * socket.onopen = () => console.log('连接成功');
 * socket.onmessage = (msg) => console.log('收到消息:', msg.data);
 * }</pre>
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * WebSocket 消息处理器
     * <p>
     * 处理连接建立、消息接收、连接关闭等事件，维护用户会话映射表
     * </p>
     */
    private final WebSocketHandlerImpl webSocketHandler;

    /**
     * WebSocket 握手拦截器
     * <p>
     * 在握手阶段提取并验证 JWT Token，将 userId 存入会话属性
     * </p>
     */
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    /**
     * 注册 WebSocket 端点和拦截器
     * <p>
     * 配置说明：
     * 1. <b>端点路径</b>：{@code /ws/notification}
     *    <ul>
     *      <li>前端通过此路径建立长连接</li>
     *      <li>需在 {@link WebMvcConfig} 中排除该路径的静态资源处理</li>
     *    </ul>
     * 
     * 2. <b>握手拦截器</b>：{@link WebSocketHandshakeInterceptor}
     *    <ul>
     *      <li>验证 JWT Token 有效性</li>
     *      <li>提取 userId 并存入 WebSocketSession 的 attributes</li>
     *    </ul>
     * 
     * 3. <b>跨域配置</b>：{@code setAllowedOriginPatterns("*")}
     *    <ul>
     *      <li>开发环境允许所有来源</li>
     *      <li>生产环境建议改为具体域名，如 {@code "https://yourdomain.com"}</li>
     *    </ul>
     * 
     * 4. <b>SockJS 支持</b>：{@code withSockJS()}
     *    <ul>
     *      <li>自动降级方案：WebSocket → SSE → 长轮询</li>
     *      <li>提升在不支持 WebSocket 的浏览器或代理环境下的兼容性</li>
     *    </ul>
     * </p>
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/notification")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        log.info("WebSocket 端点注册成功: /ws/notification (支持 SockJS)");
    }
}