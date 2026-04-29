package com.demand.module.demand.service;

import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.DemandAssignDTO;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandStatusHistory;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.demand.mapper.DemandStatusHistoryMapper;
import com.demand.module.dict.service.DictService;
import com.demand.module.notification.service.NotificationService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 需求分配业务服务
 * <p>
 * 负责将审批通过的需求分配给具体的负责人（开发人员）。
 * 分配后需求状态自动从"APPROVED"变为"IN_DEVELOPMENT"，并通知相关人员。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>分配负责人</b>：将需求分配给指定的开发人员</li>
 *   <li><b>状态流转</b>：APPROVED → IN_DEVELOPMENT</li>
 *   <li><b>审计追踪</b>：记录分配人、负责人、分配时间到数据库</li>
 *   <li><b>实时通知</b>：分配完成后通知提出者和负责人</li>
 * </ul>
 * 
 * <h3>业务流程：</h3>
 * <ol>
 *   <li>验证需求是否存在且处于"APPROVED"状态</li>
 *   <li>验证负责人是否存在</li>
 *   <li>更新需求的负责人 ID 和状态（变为"IN_DEVELOPMENT"）</li>
 *   <li>记录状态变更历史</li>
 *   <li>记录操作动态（用于时间轴展示）</li>
 *   <li>发送通知给提出者和负责人</li>
 * </ol>
 * 
 * <h3>分配规则：</h3>
 * <ul>
 *   <li>只有状态为"APPROVED"的需求才能被分配</li>
 *   <li>分配后状态自动变为"IN_DEVELOPMENT"，无需手动修改</li>
 *   <li>一个需求只能有一个负责人，重复分配会覆盖之前的负责人</li>
 *   <li>分配人通常是项目经理或管理员</li>
 * </ul>
 * 
 * <h3>通知机制：</h3>
 * <ul>
 *   <li><b>通知提出者</b>："您的需求【XXX】已被分配给 王五，请耐心等待"</li>
 *   <li><b>通知负责人</b>："您被 李四(经理) 分配负责需求【XXX】，请及时处理"</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandAssignmentService {

    private final DemandMapper demandMapper;
    private final DemandStatusHistoryMapper statusHistoryMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final DemandActivityService activityService;
    private final DictService dictService;

    /**
     * 分配需求给负责人
     * <p>
     * 将审批通过的需求分配给指定的开发人员，状态自动变为"IN_DEVELOPMENT"。
     * </p>
     * 
     * <p>
     * <b>前置条件</b>：
     * <ul>
     *   <li>需求必须存在</li>
     *   <li>需求状态必须为"APPROVED"</li>
     *   <li>负责人必须存在</li>
     *   <li>当前用户必须是项目经理或管理员</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>执行流程</b>：
     * <ol>
     *   <li>验证需求是否存在且状态正确</li>
     *   <li>验证负责人是否存在</li>
     *   <li>更新需求的负责人 ID 和状态（APPROVED→IN_DEVELOPMENT）</li>
     *   <li>记录状态变更历史（备注：分配负责人：XXX）</li>
     *   <li>记录操作动态（如："将需求分配给【王五】"）</li>
     *   <li>发送通知给提出者和负责人</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>通知示例</b>：
     * <ul>
     *   <li>通知提出者："您的需求【用户登录功能】已被分配给 王五，请耐心等待"</li>
     *   <li>通知负责人："您被 李四(经理) 分配负责需求【用户登录功能】，请及时处理"</li>
     * </ul>
     * </p>
     *
     * @param assignDTO  分配对象（包含需求ID、负责人ID）
     * @param operatorId 操作人 ID（分配人，通常是项目经理）
     * @throws BusinessException 当需求不存在、状态不正确或负责人不存在时抛出
     */
    @Transactional
    public void assignDemand(DemandAssignDTO assignDTO, Long operatorId) {
        log.info("分配需求: demandId={}, assigneeId={}, operatorId={}",
                assignDTO.getDemandId(), assignDTO.getAssigneeId(), operatorId);

        // 1. 验证需求是否存在
        Demand demand = demandMapper.findById(assignDTO.getDemandId());
        if (demand == null) {
            throw new BusinessException("需求不存在");
        }

        // 2. 验证状态（只能分配审批通过的需求）
        if (!"APPROVED".equals(demand.getStatus())) {
            throw new BusinessException("只能分配审批通过的需求");
        }

        // 3. 验证负责人是否存在
        User assignee = userMapper.findById(assignDTO.getAssigneeId());
        if (assignee == null) {
            throw new BusinessException("负责人不存在");
        }

        // 4. 更新需求状态和负责人
        String oldStatus = demand.getStatus();
        String newStatus = "IN_DEVELOPMENT";
        demand.setStatus(newStatus);
        demand.setAssigneeId(assignDTO.getAssigneeId());
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);

        // 5. 记录状态历史
        String remark = String.format("分配负责人：%s", assignee.getRealName());
        recordStatusChange(demand.getId(), oldStatus, newStatus, remark, operatorId);

        // 6. 获取操作人和状态信息
        User operator = userMapper.findById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";
        String statusText = dictService.getDictName("demand_status", newStatus);

        // 7. 记录操作动态
        String activityContent = String.format("将需求分配给【%s】", assignee.getRealName());
        activityService.saveActivity(demand.getId(), operatorId, operatorName, "ASSIGN", activityContent, null);

        // 8. 发送通知
        sendNotifications(demand, assignee, operatorId, operatorName, statusText);

        log.info("需求分配成功: demandId={}, assignee={}, status={}",
                demand.getId(), assignee.getRealName(), statusText);
    }

    /**
     * 记录状态变更历史
     * <p>
     * 将状态变更信息保存到 {@code demand_status_history} 表，用于审计追踪。
     * </p>
     *
     * @param demandId   需求 ID
     * @param oldStatus  旧状态
     * @param newStatus  新状态
     * @param remark     备注（如："分配负责人：王五"）
     * @param operatorId 操作人 ID
     */
    private void recordStatusChange(Long demandId, String oldStatus, String newStatus, String remark, Long operatorId) {
        DemandStatusHistory history = new DemandStatusHistory();
        history.setDemandId(demandId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setRemark(remark);
        history.setOperatorId(operatorId);
        history.setCreateTime(LocalDateTime.now());
        statusHistoryMapper.insert(history);
        log.debug("记录状态变更历史: demandId={}, oldStatus={}, newStatus={}", demandId, oldStatus, newStatus);
    }

    /**
     * 发送分配通知
     * <p>
     * 分配完成后，同时通知需求提出者和负责人。
     * </p>
     *
     * @param demand       需求对象
     * @param assignee     负责人对象
     * @param operatorId   操作人 ID（分配人）
     * @param operatorName 操作人姓名
     * @param statusText   状态文本（从字典获取）
     */
    private void sendNotifications(Demand demand, User assignee, Long operatorId, 
                                   String operatorName, String statusText) {
        // 1. 通知负责人
        sendNotificationToAssignee(demand, assignee, operatorName);

        // 2. 通知提出者（如果提出者与分配人不是同一人）
        if (demand.getCreatorId() != null && !demand.getCreatorId().equals(operatorId)) {
            sendNotificationToCreator(demand, operatorName, statusText, assignee.getRealName());
        }
    }

    /**
     * 发送通知给负责人
     *
     * @param demand       需求对象
     * @param assignee     负责人对象
     * @param operatorName 操作人姓名
     */
    private void sendNotificationToAssignee(Demand demand, User assignee, String operatorName) {
        String title = "需求已分配";
        String content = String.format(
                "您被 %s 分配负责需求【%s】，请及时处理。",
                operatorName,
                demand.getTitle()
        );

        notificationService.sendNotification(
                demand.getCreatorId(),  // 发送人：提出者（或系统）
                assignee.getId(),       // 接收人：负责人
                title,
                content,
                1,                      // 类型：需求通知
                demand.getId()          // 关联需求 ID
        );

        log.debug("已发送分配通知给负责人: demandId={}, assigneeId={}",
                demand.getId(), assignee.getId());
    }

    /**
     * 发送通知给提出者
     *
     * @param demand       需求对象
     * @param operatorName 操作人姓名
     * @param statusText   状态文本
     * @param assigneeName 负责人姓名
     */
    private void sendNotificationToCreator(Demand demand, String operatorName, 
                                           String statusText, String assigneeName) {
        String title = "需求已分配";
        String content = String.format(
                "您的需求【%s】已由 %s 分配给 %s，状态变更为【%s】，请耐心等待。",
                demand.getTitle(),
                operatorName,
                assigneeName,
                statusText
        );

        notificationService.sendNotification(
                demand.getCreatorId(),  // 发送人：分配人
                demand.getCreatorId(),  // 接收人：提出者
                title,
                content,
                1,                      // 类型：需求通知
                demand.getId()          // 关联需求 ID
        );

        log.debug("已发送分配通知给提出者: demandId={}, creatorId={}",
                demand.getId(), demand.getCreatorId());
    }
}
