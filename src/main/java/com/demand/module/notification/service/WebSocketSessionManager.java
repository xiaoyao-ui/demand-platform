package com.demand.module.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.TextMessage;

/**
 * WebSocket 会话管理器
 * <p>
 * 维护用户 ID 与 WebSocket 会话的映射关系，提供会话管理和消息推送功能。
 * 使用 {@link ConcurrentHashMap} 保证线程安全，支持高并发场景。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>会话管理</b>：添加、移除、查询用户会话</li>
 *   <li><b>在线检测</b>：判断用户是否在线</li>
 *   <li><b>消息推送</b>：向指定用户发送 WebSocket 消息</li>
 * </ul>
 * 
 * <h3>生命周期：</h3>
 * <ul>
 *   <li><b>连接建立</b>：用户登录成功后，WebSocket 握手时调用 {@link #addSession}</li>
 *   <li><b>连接断开</b>：用户退出或网络异常时调用 {@link #removeSession}</li>
 *   <li><b>消息推送</b>：业务逻辑调用 {@link #sendMessage} 实时推送</li>
 * </ul>
 * 
 * <h3>线程安全：</h3>
 * <p>
 * 使用 {@link ConcurrentHashMap} 存储会话映射，支持多线程并发读写。
 * 在高并发场景下（如大量用户同时在线），能保证数据一致性。
 * </p>
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 用户 ID 到 WebSocket Session 的映射表
     * <p>
     * Key: 用户 ID<br>
     * Value: WebSocket Session 对象
     * </p>
     */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 添加用户会话
     * <p>
     * 在 WebSocket 握手成功后调用，将用户 ID 与 Session 绑定。
     * </p>
     * 
     * <p>
     * <b>调用时机</b>：{@link com.demand.config.WebSocketHandlerImpl#afterConnectionEstablished}
     * </p>
     *
     * @param userId  用户 ID
     * @param session WebSocket 会话对象
     */
    public void addSession(Long userId, WebSocketSession session) {
        userSessions.put(userId, session);
        log.info("用户 {} WebSocket 连接已建立，当前在线人数: {}", userId, userSessions.size());
    }

    /**
     * 移除用户会话
     * <p>
     * 在 WebSocket 连接关闭时调用，解除用户 ID 与 Session 的绑定。
     * </p>
     * 
     * <p>
     * <b>调用时机</b>：{@link com.demand.config.WebSocketHandlerImpl#afterConnectionClosed}
     * </p>
     *
     * @param userId 用户 ID
     */
    public void removeSession(Long userId) {
        userSessions.remove(userId);
        log.info("用户 {} WebSocket 连接已断开，当前在线人数: {}", userId, userSessions.size());
    }

    /**
     * 获取用户会话
     * <p>
     * 根据用户 ID 查询对应的 WebSocket Session，用于消息推送。
     * </p>
     *
     * @param userId 用户 ID
     * @return WebSocket Session，如果用户不在线则返回 null
     */
    public WebSocketSession getSession(Long userId) {
        return userSessions.get(userId);
    }

    /**
     * 检查用户是否在线
     * <p>
     * 判断用户是否有活跃的 WebSocket 连接。
     * </p>
     *
     * @param userId 用户 ID
     * @return true 表示在线，false 表示离线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线用户数量
     * <p>
     * 用于监控面板展示当前系统的活跃用户数。
     * </p>
     *
     * @return 在线用户数量
     */
    public int getOnlineCount() {
        return userSessions.size();
    }

    /**
     * 向指定用户推送消息
     * <p>
     * 检查用户是否在线，然后将消息通过 WebSocket 发送出去。
     * 如果用户不在线或连接已关闭，记录警告日志但不抛出异常。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>新通知实时推送</li>
     *   <li>需求状态变更提醒</li>
     *   <li>系统公告广播</li>
     * </ul>
     * </p>
     *
     * @param userId  用户 ID
     * @param message 消息内容（JSON 字符串）
     */
    public void sendMessage(Long userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("向用户 {} 推送消息成功", userId);
            } catch (Exception e) {
                log.error("向用户 {} 推送消息失败", userId, e);
            }
        } else {
            log.warn("用户 {} 不在线或连接已关闭，消息推送失败", userId);
        }
    }

    /**
     * 获取所有用户的会话映射
     * <p>
     * 返回当前所有在线用户的 WebSocket Session 映射表，用于广播消息等场景。
     * </p>
     * 
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *   <li>系统公告广播</li>
     *   <li>验证码实时通知（开发环境）</li>
     *   <li>全局状态变更推送</li>
     * </ul>
     * </p>
     *
     * @return 用户 ID 到 WebSocket Session 的映射表
     */
    public Map<Long, WebSocketSession> getAllSessions() {
        return userSessions;
    }
}
