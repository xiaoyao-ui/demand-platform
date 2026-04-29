package com.demand.config;

import com.demand.common.NotificationTypeEnum;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.notification.service.NotificationService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.OperationLogMapper;
import com.demand.module.user.mapper.RoleMapper;
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
     * 角色 Mapper，用于查询角色列表
     */
    private final RoleMapper roleMapper;

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
     * 每天检查待审批超过 24 小时的需求，并向项目经理和管理员发送提醒通知。
     * </p>
     * 
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>查询待审批超过 24 小时的需求</li>
     *   <li>获取所有项目经理和超级管理员的用户 ID</li>
     *   <li>为每个管理者发送汇总提醒通知</li>
     * </ol>
     * 
     * <h3>通知内容示例：</h3>
     * <pre>
     * 标题：待审批需求提醒
     * 内容：您有 8 个需求待审批超过 24 小时：
     *       - 【用户登录功能】（提出人：张三）
     *       - 【订单管理模块】（提出人：李四）
     *       ... 还有 6 个需求
     * </pre>
     */
    @Scheduled(cron = "0 0 9 * * ?") // 每天早上 9 点执行
    public void sendPendingDemandReminder() {
        log.info("开始检查待审批需求...");

        try {
            // 1. 查询待审批超过 24 小时的需求
            List<Demand> pendingDemands = fetchPendingDemands();
            
            if (pendingDemands.isEmpty()) {
                log.info("没有待审批超过 24 小时的需求");
                return;
            }
            
            log.info("发现 {} 个待审批超过 24 小时的需求", pendingDemands.size());

            // 2. 获取接收人列表（项目经理 + 超级管理员）
            Set<Long> receiverIds = fetchManagerUserIds();
            
            if (receiverIds.isEmpty()) {
                log.warn("系统中没有找到项目经理或超级管理员，无法发送提醒");
                return;
            }
            
            log.debug("找到 {} 位管理者需要接收提醒", receiverIds.size());

            // 3. 批量发送通知
            sendRemindersToManagers(pendingDemands, receiverIds);
            
            log.info("待审批需求提醒发送完成，共通知 {} 位管理者", receiverIds.size());
            
        } catch (Exception e) {
            log.error("检查待审批需求失败", e);
        }
    }

    /**
     * 查询待审批超过 24 小时的需求
     *
     * @return 待审批需求列表
     */
    private List<Demand> fetchPendingDemands() {
        List<Demand> demands = demandMapper.findPendingApprovalOverHours(24);
        return demands != null ? demands : List.of();
    }

    /**
     * 获取所有项目经理和超级管理员的用户 ID
     *
     * @return 用户 ID 集合
     */
    private Set<Long> fetchManagerUserIds() {
        // 1. 查询角色 ID
        List<Long> projectManagerRoleIds = roleMapper.selectRoleIdsByKeys(List.of("PROJECT_MANAGER"));
        List<Long> superAdminRoleIds = roleMapper.selectRoleIdsByKeys(List.of("SUPER_ADMIN"));

        // 2. 合并角色 ID（去重）
        Set<Long> targetRoleIds = new java.util.HashSet<>();
        targetRoleIds.addAll(projectManagerRoleIds != null ? projectManagerRoleIds : List.of());
        targetRoleIds.addAll(superAdminRoleIds != null ? superAdminRoleIds : List.of());

        if (targetRoleIds.isEmpty()) {
            log.warn("系统中没有找到 PROJECT_MANAGER 或 SUPER_ADMIN 角色定义");
            return Set.of();
        }

        // 3. 查询拥有这些角色的所有用户 ID
        Set<Long> receiverIds = new java.util.HashSet<>();
        for (Long roleId : targetRoleIds) {
            List<Long> userIds = roleMapper.selectUserIdsByRoleId(roleId);
            if (userIds != null && !userIds.isEmpty()) {
                receiverIds.addAll(userIds);
            }
        }
        
        return receiverIds;
    }

    /**
     * 向管理者发送提醒通知
     *
     * @param pendingDemands 待审批需求列表
     * @param receiverIds 接收人 ID 集合
     */
    private void sendRemindersToManagers(List<Demand> pendingDemands, Set<Long> receiverIds) {
        // 构建通知内容（只构建一次，复用给所有接收人）
        String title = "待审批需求提醒";
        String content = buildReminderContent(pendingDemands);
        
        // 批量发送通知
        for (Long receiverId : receiverIds) {
            try {
                notificationService.sendNotification(
                    0L,                    // 发送人：系统
                    receiverId,            // 接收人：管理者
                    title,                 // 标题
                    content,               // 内容
                    NotificationTypeEnum.APPROVAL_REMINDER.getCode(),  // 类型：审批提醒
                    null                   // 无关联 ID
                );
                log.debug("已发送提醒给管理者: userId={}", receiverId);
            } catch (Exception e) {
                // 单个用户发送失败不影响其他用户
                log.error("发送提醒给管理者失败: userId={}", receiverId, e);
            }
        }
    }

    /**
     * 构建提醒通知内容
     *
     * @param pendingDemands 待审批需求列表
     * @return 格式化的通知内容
     */
    private String buildReminderContent(List<Demand> pendingDemands) {
        StringBuilder content = new StringBuilder();
        content.append("您有 ").append(pendingDemands.size()).append(" 个需求待审批超过 24 小时：\n");
        
        // 列出前 5 个需求的详细信息
        int displayLimit = Math.min(5, pendingDemands.size());
        for (int i = 0; i < displayLimit; i++) {
            Demand demand = pendingDemands.get(i);
            String proposerName = demand.getCreatorName() != null ? demand.getCreatorName() : "未知";
            content.append("- 【").append(demand.getTitle()).append("】");
            content.append("（提出人：").append(proposerName).append("）\n");
        }
        
        // 如果超过 5 个，显示剩余数量
        if (pendingDemands.size() > 5) {
            content.append("... 还有 ").append(pendingDemands.size() - 5).append(" 个需求");
        }
        
        return content.toString();
    }
}
