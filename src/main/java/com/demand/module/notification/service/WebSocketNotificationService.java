package com.demand.module.notification.service;

import com.demand.module.notification.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 消息推送服务
 * <p>
 * 封装 WebSocket 消息推送逻辑，用于向指定用户发送实时通知。
 * 此服务已被整合到 {@link NotificationService} 中，作为备用方案保留。
 * </p>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>需求审批通过后实时通知提交人</li>
 *   <li>任务分配时实时通知负责人</li>
 *   <li>评论回复时实时通知被回复人</li>
 * </ul>
 * 
 * <h3>注意事项：</h3>
 * <p>
 * 目前 {@link NotificationService#sendNotification} 已直接使用 {@link WebSocketSessionManager}，
 * 此类可作为独立推送服务的参考实现。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    /**
     * WebSocket 会话管理器
     */
    private final WebSocketSessionManager sessionManager;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 向指定用户推送新通知
     * <p>
     * 检查用户是否在线且连接正常，然后发送 JSON 格式的通知消息。
     * </p>
     * 
     * <p>
     * <b>推送流程</b>：
     * <ol>
     *   <li>从会话管理器中获取用户的 WebSocket Session</li>
     *   <li>检查 Session 是否存在且处于打开状态</li>
     *   <li>将通知对象序列化为 JSON 字符串</li>
     *   <li>通过 WebSocket 发送文本消息</li>
     * </ol>
     * </p>
     *
     * @param userId       用户 ID
     * @param notification 通知对象
     */
    public void pushNotification(Long userId, Notification notification) {
        try {
            // 1. 获取用户的 WebSocket Session
            WebSocketSession session = sessionManager.getSession(userId);

            // 2. 检查连接状态
            if (session != null && session.isOpen()) {
                // 3. 序列化通知对象
                String message = objectMapper.writeValueAsString(notification);
                
                // 4. 发送消息
                session.sendMessage(new TextMessage(message));
                log.info("向用户 {} 推送通知: {}", userId, notification.getTitle());
            } else {
                log.warn("用户 {} 不在线或连接已关闭，无法推送通知", userId);
            }
        } catch (Exception e) {
            log.error("向用户 {} 推送通知失败", userId, e);
        }
    }
}
