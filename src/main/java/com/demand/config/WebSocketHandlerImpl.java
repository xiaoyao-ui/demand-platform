package com.demand.config;

import com.demand.module.notification.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 消息处理器 - 文本消息类型
 * <p>
 * 继承自 {@link TextWebSocketHandler}，负责处理 WebSocket 连接的生命周期事件，
 * 包括连接建立、消息接收、连接关闭和异常处理。通过 {@link WebSocketSessionManager} 管理用户会话映射。
 * </p>
 * 
 * <h3>核心职责：</h3>
 * <ul>
 *   <li><b>连接管理</b>：在用户建立/断开连接时，维护 userId 与 WebSocketSession 的映射关系</li>
 *   <li><b>消息处理</b>：接收客户端发送的消息（当前仅记录日志，预留扩展）</li>
 *   <li><b>异常恢复</b>：当发生传输错误时，主动关闭会话并清理资源</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>实时通知推送：审批提醒、需求状态变更、评论回复等</li>
 *   <li>在线状态监控：统计当前在线用户数</li>
 *   <li>双向通信：未来可扩展客户端向服务端发送消息的功能</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandlerImpl extends TextWebSocketHandler {

    /**
     * WebSocket 会话管理器
     * <p>
     * 维护 userId 与 WebSocketSession 的映射表，支持根据用户 ID 精准推送消息
     * </p>
     */
    private final WebSocketSessionManager sessionManager;

    /**
     * 连接建立后的回调
     * <p>
     * 工作流程：
     * 1. 从会话属性中提取 userId（由 {@link WebSocketAuthInterceptor} 在握手阶段存入）
     * 2. 调用 {@link WebSocketSessionManager#addSession} 注册会话
     * 3. 记录在线用户数到日志
     * </p>
     * 
     * <p>
     * <b>注意：</b>如果 userId 为 null，说明握手拦截器未正确执行，此时拒绝建立连接。
     * </p>
     *
     * @param session 新建立的 WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("用户 {} WebSocket 连接已建立，当前在线用户数: {}", userId, sessionManager.getOnlineCount());
        }
    }

    /**
     * 处理客户端发送的文本消息
     * <p>
     * 当前实现仅记录日志，预留扩展空间。未来可以支持：
     * <ul>
     *   <li>心跳检测：接收客户端的心跳包，保持连接活跃</li>
     *   <li>即时聊天：用户之间发送私信或群聊消息</li>
     *   <li>命令交互：客户端发送控制指令（如订阅特定频道）</li>
     * </ul>
     * </p>
     *
     * @param session 当前 WebSocket 会话
     * @param message 接收到的文本消息
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理客户端发送的消息（如果需要的话）
        log.debug("收到 WebSocket 消息: {}", message.getPayload());
    }

    /**
     * 连接关闭后的回调
     * <p>
     * 工作流程：
     * 1. 从会话属性中提取 userId
     * 2. 调用 {@link WebSocketSessionManager#removeSession} 移除会话
     * 3. 记录在线用户数到日志
     * </p>
     * 
     * <p>
     * <b>触发场景：</b>
     * <ul>
     *   <li>用户主动关闭浏览器标签页</li>
     *   <li>网络中断导致连接超时</li>
     *   <li>服务端主动关闭连接（如用户被踢下线）</li>
     * </ul>
     * </p>
     *
     * @param session 已关闭的 WebSocket 会话
     * @param status  关闭状态码和原因
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("用户 {} WebSocket 连接已关闭，当前在线用户数: {}", userId, sessionManager.getOnlineCount());
        }
    }

    /**
     * 传输异常处理
     * <p>
     * 当 WebSocket 通信过程中发生异常时触发，例如：
     * <ul>
     *   <li>网络抖动导致数据包损坏</li>
     *   <li>客户端突然断网</li>
     *   <li>消息格式不符合协议规范</li>
     * </ul>
     * </p>
     * 
     * <p>
     * 处理策略：
     * 1. 记录异常日志（包含 userId 和堆栈信息）
     * 2. 如果会话仍处于打开状态，主动关闭以释放资源
     * 3. 会话管理器会在 {@link #afterConnectionClosed} 中清理映射关系
     * </p>
     *
     * @param session   发生异常的 WebSocket 会话
     * @param exception 捕获到的异常对象
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("用户 {} WebSocket 传输异常", userId, exception);
        if (session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("关闭 WebSocket 会话失败", e);
            }
        }
    }
}
