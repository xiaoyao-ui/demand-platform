package com.demand.module.notification.service;

import com.demand.common.PageResult;
import com.demand.module.notification.entity.Notification;
import com.demand.module.notification.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知消息业务逻辑服务
 * <p>
 * 负责通知的创建、查询和标记已读。
 * 集成 WebSocket 实现新通知的实时推送。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>发送通知</b>：创建通知并通过 WebSocket 推送给在线用户</li>
 *   <li><b>分页查询</b>：支持分页加载，避免一次性返回过多数据</li>
 *   <li><b>标记已读</b>：单条、批量或全部标记为已读</li>
 * </ul>
 * 
 * <h3>实时推送机制：</h3>
 * <p>
 * 当创建新通知时，系统会尝试通过 WebSocket 实时推送给接收人：
 * <ul>
 *   <li>如果用户在线：立即收到推送，前端弹出提示框</li>
 *   <li>如果用户离线：通知存储在数据库中，下次登录时自动加载</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * 通知数据访问层
     */
    private final NotificationMapper notificationMapper;

    /**
     * WebSocket 会话管理器，用于实时推送
     */
    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * JSON 序列化工具，用于将通知对象转换为 JSON 字符串
     */
    private final ObjectMapper objectMapper;

    /**
     * 发送通知
     * <p>
     * 创建通知并尝试通过 WebSocket 实时推送给接收人。
     * 推送失败不影响通知的存储，确保消息不丢失。
     * </p>
     * 
     * <p>
     * <b>业务流程</b>：
     * <ol>
     *   <li>创建通知对象，设置发送人、接收人、标题、内容等字段</li>
     *   <li>插入数据库，持久化存储</li>
     *   <li>尝试通过 WebSocket 推送给在线用户</li>
     *   <li>如果推送失败，记录日志但不影响主流程</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>使用示例</b>：
     * <pre>{@code
     * // 需求审批通过后通知提交人
     * notificationService.sendNotification(
     *     managerId,      // 发送人：项目经理
     *     proposerId,     // 接收人：需求提交人
     *     "需求审批通过",   // 标题
     *     "您的需求'用户登录'已通过审批", // 内容
     *     1,              // 类型：需求审批
     *     demandId        // 关联 ID：需求 ID
     * );
     * }</pre>
     * </p>
     *
     * @param userId     发送人 ID
     * @param receiverId 接收人 ID
     * @param title      通知标题
     * @param content    通知内容
     * @param type       通知类型
     * @param relatedId  关联的业务对象 ID
     */
    public void sendNotification(Long userId, Long receiverId, String title, String content, Integer type, Long relatedId) {
        log.info("发送通知: userId={}, receiverId={}, title={}", userId, receiverId, title);
        
        // 1. 创建通知对象
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setReceiverId(receiverId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        
        // 2. 插入数据库
        notificationMapper.insert(notification);
        
        // 3. 通过 WebSocket 实时推送给接收人
        try {
            String message = objectMapper.writeValueAsString(notification);
            webSocketSessionManager.sendMessage(receiverId, message);
        } catch (Exception e) {
            log.error("WebSocket 推送通知失败: receiverId={}, title={}", receiverId, title, e);
        }
    }

    /**
     * 分页查询通知列表
     * <p>
     * 返回当前用户的所有通知，按创建时间倒序排列。
     * 对分页参数进行校验，防止非法输入。
     * </p>
     * 
     * <p>
     * <b>参数校验</b>：
     * <ul>
     *   <li>{@code pageNum} 默认为 1，最小值为 1</li>
     *   <li>{@code pageSize} 默认为 10，范围为 1-100</li>
     * </ul>
     * </p>
     *
     * @param receiverId 接收人 ID
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    public PageResult<Notification> getNotifications(Long receiverId, Integer pageNum, Integer pageSize) {
        // 1. 参数校验
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        
        log.debug("查询用户通知: receiverId={}, pageNum={}, pageSize={}", receiverId, pageNum, pageSize);
        
        // 2. 计算偏移量
        Integer offset = (pageNum - 1) * pageSize;
        
        // 3. 查询数据
        List<Notification> list = notificationMapper.findByReceiverId(receiverId, offset, pageSize);
        Long total = notificationMapper.countByReceiverId(receiverId);
        
        log.debug("查询通知结果: total={}", total);
        return new PageResult<>(list, total, pageNum, pageSize);
    }

    /**
     * 查询未读通知数量
     * <p>
     * 用于前端在通知图标上显示红色角标。
     * </p>
     *
     * @param receiverId 接收人 ID
     * @return 未读通知数量
     */
    public Long getUnreadCount(Long receiverId) {
        log.debug("查询未读通知数量: receiverId={}", receiverId);
        return notificationMapper.countUnreadByReceiverId(receiverId);
    }

    /**
     * 标记单条通知为已读
     *
     * @param id 通知 ID
     */
    public void markAsRead(Long id) {
        log.info("标记通知为已读: notificationId={}", id);
        notificationMapper.markAsRead(id);
    }

    /**
     * 标记全部通知为已读
     *
     * @param receiverId 接收人 ID
     */
    public void markAllAsRead(Long receiverId) {
        log.info("标记全部通知为已读: receiverId={}", receiverId);
        notificationMapper.markAllAsRead(receiverId);
    }

    /**
     * 批量标记通知为已读
     *
     * @param ids 通知 ID 列表
     */
    public void markBatchAsRead(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        notificationMapper.batchMarkAsRead(ids);
    }
}
