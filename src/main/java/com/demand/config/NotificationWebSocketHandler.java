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
 * 消息通知 WebSocket 处理器
 * <p>
 * 负责处理客户端与服务器之间的长连接生命周期管理。
 * 当用户登录成功后，前端会建立 WebSocket 连接，后端通过此处理器：
 * 1. 建立连接时：绑定用户 ID 与 Session，记录在线状态
 * 2. 断开连接时：清理 Session 缓存，更新在线人数
 * 3. 异常处理时：记录错误日志，方便排查网络问题
 * </p>
 * <p>
 * 应用场景：
 * - 需求状态变更时，实时推送通知给相关用户
 * - 审批流程流转时，提醒负责人及时处理
 * - 评论/附件更新时，通知协作者
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    /**
     * 连接建立成功后的回调
     * <p>
     * 从 Session 属性中获取握手阶段注入的 userId，
     * 将其与当前 WebSocketSession 绑定并注册到 SessionManager 中。
     * </p>
     *
     * @param session 新建立的 WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("用户 {} 建立 WebSocket 连接，当前在线用户数: {}",
                    userId, sessionManager.getOnlineCount());
        }
    }

    /**
     * 处理客户端发送的文本消息
     * <p>
     * 目前主要用于接收前端的心跳包（Ping/Pong），保持连接活跃。
     * 若后续需要实现客户端主动发送消息给后端，可在此扩展逻辑。
     * </p>
     *
     * @param session 当前 WebSocket 会话
     * @param message 接收到的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("收到客户端消息: {}", message.getPayload());
    }

    /**
     * 连接关闭后的回调
     * <p>
     * 用户退出登录、关闭浏览器或网络断开时触发。
     * 清理 SessionManager 中的无效连接，防止内存泄漏。
     * </p>
     *
     * @param session  即将关闭的 WebSocket 会话
     * @param status   关闭状态（包含关闭码和原因）
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("用户 {} 断开 WebSocket 连接，当前在线用户数: {}",
                    userId, sessionManager.getOnlineCount());
        }
    }

    /**
     * 传输异常处理
     * <p>
     * 当网络抖动、消息格式错误或底层 Socket 异常时触发。
     * 记录异常堆栈以便排查问题，不阻断其他用户的连接。
     * </p>
     *
     * @param session   发生异常的 WebSocket 会话
     * @param exception 捕获到的异常对象
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("用户 {} WebSocket 传输异常", userId, exception);
    }
}
