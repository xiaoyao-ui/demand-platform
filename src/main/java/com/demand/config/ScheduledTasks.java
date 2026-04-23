package com.demand.config;

import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.notification.service.NotificationService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.OperationLogMapper;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 系统定时任务调度器
 * <p>
 * 负责执行周期性维护任务，包括验证码清理、日志归档、需求审批提醒等。
 * 通过 {@link EnableScheduling} 启用 Spring 定时任务功能，使用 Cron 表达式定义执行时间。
 * </p>
 * 
 * <h3>当前支持的定时任务：</h3>
 * <ul>
 *   <li>每天凌晨 2:00 - 清理 Redis 中的过期验证码</li>
 *   <li>每天凌晨 3:00 - 清理 90 天前的操作日志</li>
 *   <li>每小时整点 - 检查待审批需求并发送提醒</li>
 * </ul>
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    /**
     * Redis 模板，用于操作验证码等缓存数据
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 操作日志 Mapper，用于清理历史日志
     */
    private final OperationLogMapper operationLogMapper;

    /**
     * 需求 Mapper，用于查询待审批需求
     */
    private final DemandMapper demandMapper;

    /**
     * 用户 Mapper，用于查询项目经理列表
     */
    private final UserMapper userMapper;

    /**
     * 通知服务，用于发送审批提醒
     */
    private final NotificationService notificationService;

    /**
     * 清理过期的验证码
     * <p>
     * 执行时间：每天凌晨 2:00
     * Cron 表达式：{@code 0 0 2 * * ?}
     * </p>
     * 
     * <p>
     * 工作原理：
     * 1. 扫描所有以 "verification:" 开头的 Redis Key
     * 2. 批量删除这些 Key（Redis 已设置 TTL，此处为兜底清理）
     * 3. 记录删除数量到日志
     * </p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredVerificationCodes() {
        log.info("开始清理过期验证码...");

        try {
            Set<String> keys = redisTemplate.keys("verification:*");
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.info("清理过期验证码完成，删除数量：{}", deletedCount);
            } else {
                log.info("没有需要清理的验证码");
            }
        } catch (Exception e) {
            log.error("清理过期验证码失败：", e);
        }
    }

    /**
     * 清理历史操作日志
     * <p>
     * 执行时间：每天凌晨 3:00
     * Cron 表达式：{@code 0 0 3 * * ?}
     * </p>
     * 
     * <p>
     * 清理策略：
     * 保留最近 90 天的操作日志，删除更早的记录以释放数据库空间。
     * 适用于审计日志、访问记录等非关键历史数据。
     * </p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldOperationLogs() {
        log.info("开始清理过期操作日志...");

        try {
            // 计算 90 天前的时间点
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90);
            
            // 执行删除操作
            int deletedCount = operationLogMapper.deleteBeforeTime(cutoffTime);
            
            log.info("清理过期操作日志完成，删除 {} 条记录（{} 天前的数据）", deletedCount, 90);
        } catch (Exception e) {
            log.error("清理过期操作日志失败：", e);
        }
    }

    /**
     * 发送待审批需求提醒
     * <p>
     * 执行时间：每小时整点
     * Cron 表达式：{@code 0 0 * * * ?}
     * </p>
     * 
     * <p>
     * 提醒规则：
     * 1. 查询状态为"待审批"且创建时间超过 24 小时的需求
     * 2. 向所有项目经理（role=2）和管理员（role=3）发送站内通知
     * 3. 记录提醒次数到日志，避免频繁打扰
     * </p>
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void sendPendingDemandReminder() {
        log.info("检查待审批需求...");

        try {
            // 1. 查询待审批超过 24 小时的需求
            List<Demand> pendingDemands = demandMapper.findPendingApprovalOverHours(24);
            
            if (pendingDemands == null || pendingDemands.isEmpty()) {
                log.info("没有待审批超过 24 小时的需求");
                return;
            }
            
            log.info("发现 {} 个待审批超过 24 小时的需求", pendingDemands.size());
            
            // 2. 查询所有项目经理（role=2）和管理员（role=3）
            List<User> projectManagers = userMapper.findByRole(2);
            List<User> admins = userMapper.findByRole(3);
            
            // 合并接收人列表（去重）
            Set<Long> receiverIds = new java.util.HashSet<>();
            projectManagers.forEach(user -> receiverIds.add(user.getId()));
            admins.forEach(user -> receiverIds.add(user.getId()));
            
            if (receiverIds.isEmpty()) {
                log.warn("系统中没有项目经理或管理员，无法发送提醒");
                return;
            }
            
            // 3. 向每个管理者发送提醒通知
            for (Long receiverId : receiverIds) {
                StringBuilder title = new StringBuilder("待审批需求提醒");
                StringBuilder content = new StringBuilder();
                content.append("您有 ").append(pendingDemands.size()).append(" 个需求待审批超过 24 小时：\n");
                
                // 列出前 5 个需求的标题
                int limit = Math.min(5, pendingDemands.size());
                for (int i = 0; i < limit; i++) {
                    Demand demand = pendingDemands.get(i);
                    content.append("- 【").append(demand.getTitle()).append("】");
                    content.append("（提出人：").append(demand.getProposerName()).append("）\n");
                }
                
                if (pendingDemands.size() > 5) {
                    content.append("... 还有 ").append(pendingDemands.size() - 5).append(" 个需求");
                }
                
                // 发送通知（userId=0 表示系统通知，receiverId=接收人ID）
                notificationService.sendNotification(
                    0L,           // 系统发送
                    receiverId,   // 接收人
                    title.toString(),
                    content.toString(),
                    3,            // 类型：3-审批提醒
                    null          // 无关联 ID
                );
            }
            
            log.info("待审批需求提醒发送完成，共通知 {} 位管理者", receiverIds.size());
        } catch (Exception e) {
            log.error("检查待审批需求失败：", e);
        }
    }
}
