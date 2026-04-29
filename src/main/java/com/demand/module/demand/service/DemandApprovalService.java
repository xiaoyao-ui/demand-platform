package com.demand.module.demand.service;

import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.DemandApproveDTO;
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
 * 需求审批业务服务
 * <p>
 * 负责处理项目经理对待审批需求的审批操作，包括通过和拒绝两种结果。
 * 审批完成后会自动记录状态历史、操作动态，并发送通知给需求提出者。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>审批需求</b>：项目经理对待审批的需求进行审批（通过/拒绝）</li>
 *   <li><b>状态流转</b>：PENDING_REVIEW → APPROVED 或 REJECTED</li>
 *   <li><b>审计追踪</b>：记录审批人、审批时间、审批意见到数据库</li>
 *   <li><b>实时通知</b>：审批完成后立即通知需求提出者</li>
 * </ul>
 * 
 * <h3>业务流程：</h3>
 * <ol>
 *   <li>验证需求是否存在且处于"PENDING_REVIEW"状态</li>
 *   <li>根据审批结果更新需求状态（通过→APPROVED，拒绝→REJECTED）</li>
 *   <li>记录审批人 ID、审批时间和审批意见</li>
 *   <li>记录状态变更历史（包含审批意见）</li>
 *   <li>记录操作动态（用于时间轴展示）</li>
 *   <li>发送通知给需求提出者</li>
 * </ol>
 * 
 * <h3>审批规则：</h3>
 * <ul>
 *   <li>只有状态为"PENDING_REVIEW"的需求才能被审批</li>
 *   <li>审批通过后，需求可以进入分配阶段</li>
 *   <li>审批拒绝后，提出人可以修改内容并重新提交</li>
 *   <li>审批意见选填，但建议填写具体的审批说明</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandApprovalService {
    
    private final DemandMapper demandMapper;
    private final DemandStatusHistoryMapper statusHistoryMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final DemandActivityService activityService;
    private final DictService dictService;
    
    /**
     * 审批需求
     * <p>
     * 项目经理或管理员对待审批的需求进行审批，可以选择通过或拒绝。
     * 审批通过后状态变为"APPROVED"，拒绝后变为"REJECTED"。
     * </p>
     * 
     * <p>
     * <b>前置条件</b>：
     * <ul>
     *   <li>需求必须存在</li>
     *   <li>需求状态必须为"PENDING_REVIEW"</li>
     *   <li>当前用户必须是项目经理或管理员</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>执行流程</b>：
     * <ol>
     *   <li>验证需求是否存在且状态正确</li>
     *   <li>更新需求状态、审批人、审批时间、审批意见</li>
     *   <li>记录状态变更历史</li>
     *   <li>记录操作动态（如："审批通过了需求（意见：同意）"）</li>
     *   <li>发送通知给需求提出者</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>通知示例</b>：
     * <ul>
     *   <li>通过："您的需求【用户登录功能】已由 李四(经理) 审批通过。审批意见：需求描述清晰，同意开发"</li>
     *   <li>拒绝："您的需求【用户登录功能】已由 李四(经理) 拒绝。审批意见：缺少详细的接口文档"</li>
     * </ul>
     * </p>
     *
     * @param approveDTO  审批对象（包含需求ID、是否通过、审批意见）
     * @param approverId  审批人 ID（项目经理或管理员）
     * @throws BusinessException 当需求不存在或状态不正确时抛出
     */
    @Transactional
    public void approveDemand(DemandApproveDTO approveDTO, Long approverId) {
        log.info("审批需求: demandId={}, approverId={}, approved={}", 
                approveDTO.getDemandId(), approverId, approveDTO.getApproved());
        
        // 1. 验证需求是否存在
        Demand demand = demandMapper.findById(approveDTO.getDemandId());
        if (demand == null) {
            throw new BusinessException("需求不存在");
        }
        
        // 2. 验证状态（只能审批待审批的需求）
        if (!"PENDING_REVIEW".equals(demand.getStatus())) {
            throw new BusinessException("只能审批待审批状态的需求");
        }
        
        // 3. 计算新状态（通过→APPROVED，拒绝→REJECTED）
        String oldStatus = demand.getStatus();
        String newStatus = approveDTO.getApproved() ? "APPROVED" : "REJECTED";
        
        // 4. 更新需求信息
        demand.setStatus(newStatus);
        demand.setApproverId(approverId);
        demand.setApproveTime(LocalDateTime.now());
        demand.setApproveComment(approveDTO.getComment());
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);
        
        // 5. 记录状态变更历史
        recordStatusChange(approveDTO.getDemandId(), oldStatus, newStatus, approveDTO.getComment(), approverId);
        
        // 6. 获取操作人和状态信息
        User approver = userMapper.findById(approverId);
        String approverName = approver != null ? approver.getRealName() : "系统";
        String resultText = approveDTO.getApproved() ? "通过" : "拒绝";
        String statusText = dictService.getDictName("demand_status", newStatus);
        
        // 7. 记录操作动态
        String activityContent = buildActivityContent(resultText, approveDTO.getComment());
        activityService.saveActivity(approveDTO.getDemandId(), approverId, approverName, "APPROVE", activityContent, null);
        
        // 8. 发送通知给提出者
        sendNotification(demand, approverId, approverName, approveDTO.getApproved(), statusText, approveDTO.getComment());
        
        log.info("需求审批完成: demandId={}, status={}, approver={}", 
                demand.getId(), statusText, approverId);
    }
    
    /**
     * 构建操作动态内容
     *
     * @param resultText 审批结果文本（通过/拒绝）
     * @param comment    审批意见
     * @return 操作动态内容
     */
    private String buildActivityContent(String resultText, String comment) {
        if (comment != null && !comment.isEmpty()) {
            return String.format("审批%s了需求（意见：%s）", resultText, comment);
        }
        return String.format("审批%s了需求", resultText);
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
     * @param remark     备注（审批意见）
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
     * 发送审批通知
     * <p>
     * 审批完成后，向需求提出者发送通知，告知审批结果和审批意见。
     * 如果审批人就是提出者，则不发送通知。
     * </p>
     *
     * @param demand      需求对象
     * @param approverId  审批人 ID
     * @param approverName 审批人姓名
     * @param approved    是否通过
     * @param statusText  状态文本（从字典获取）
     * @param comment     审批意见
     */
    private void sendNotification(Demand demand, Long approverId, String approverName, 
                                  Boolean approved, String statusText, String comment) {
        // 如果审批人就是提出者，不发送通知
        if (demand.getCreatorId() == null || demand.getCreatorId().equals(approverId)) {
            log.debug("审批人即为提出者，跳过通知: demandId={}, approverId={}", 
                    demand.getId(), approverId);
            return;
        }
        
        String title = approved ? "需求审批通过" : "需求被拒绝";
        String commentText = comment != null && !comment.isEmpty() ? comment : "无";
        String content = String.format(
                "您的需求【%s】已由 %s %s。审批意见：%s",
                demand.getTitle(), approverName, statusText, commentText
        );
        
        notificationService.sendNotification(
                approverId,              // 发送人：审批人
                demand.getCreatorId(),   // 接收人：提出者
                title,
                content,
                1,                       // 类型：需求通知
                demand.getId()           // 关联需求 ID
        );
        
        log.debug("已发送审批通知给提出者: demandId={}, proposerId={}", 
                demand.getId(), demand.getCreatorId());
    }
}
