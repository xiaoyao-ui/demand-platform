package com.demand.module.demand.service;

import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.DemandApproveDTO;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandStatusHistory;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.demand.mapper.DemandStatusHistoryMapper;
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
 *   <li><b>状态流转</b>：待审批(0) → 审批通过(1) 或 已拒绝(5)</li>
 *   <li><b>审计追踪</b>：记录审批人、审批时间、审批意见到数据库</li>
 *   <li><b>实时通知</b>：审批完成后立即通知需求提出者</li>
 * </ul>
 * 
 * <h3>业务流程：</h3>
 * <ol>
 *   <li>验证需求是否存在且处于"待审批"状态</li>
 *   <li>根据审批结果更新需求状态（通过→1，拒绝→5）</li>
 *   <li>记录审批人 ID、审批时间和审批意见</li>
 *   <li>记录状态变更历史（包含审批意见）</li>
 *   <li>记录操作动态（用于时间轴展示）</li>
 *   <li>发送通知给需求提出者</li>
 * </ol>
 * 
 * <h3>审批规则：</h3>
 * <ul>
 *   <li>只有状态为"待审批"(0) 的需求才能被审批</li>
 *   <li>审批通过后，需求可以进入分配阶段</li>
 *   <li>审批拒绝后，提出人可以修改内容并重新提交</li>
 *   <li>审批意见选填，但建议填写具体的审批说明</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandApprovalService {
    
    /**
     * 需求数据访问层
     */
    private final DemandMapper demandMapper;

    /**
     * 状态历史数据访问层
     */
    private final DemandStatusHistoryMapper statusHistoryMapper;

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * 通知服务
     */
    private final NotificationService notificationService;

    /**
     * 需求动态服务
     */
    private final DemandActivityService activityService;
    
    /**
     * 审批需求
     * <p>
     * 项目经理对待审批的需求进行审批，可以选择通过或拒绝。
     * 审批通过后状态变为"审批通过"(1)，拒绝后变为"已拒绝"(5)。
     * </p>
     * 
     * <p>
     * <b>前置条件</b>：
     * <ul>
     *   <li>需求必须存在</li>
     *   <li>需求状态必须为"待审批"(0)</li>
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
        if (demand.getStatus() != 0) {
            throw new BusinessException("只能审批待审批状态的需求");
        }
        
        // 3. 计算新状态（通过→1，拒绝→5）
        Integer oldStatus = demand.getStatus();
        Integer newStatus = approveDTO.getApproved() ? 1 : 5;
        
        // 4. 更新需求信息
        demand.setStatus(newStatus);
        demand.setApproverId(approverId);
        demand.setApproveTime(LocalDateTime.now());
        demand.setApproveComment(approveDTO.getComment());
        demand.setUpdateTime(LocalDateTime.now());
        
        demandMapper.update(demand);
        
        // 5. 记录状态变更历史
        recordStatusChange(approveDTO.getDemandId(), oldStatus, newStatus, approveDTO.getComment(), approverId);
        
        // 6. 记录操作动态
        User approver = userMapper.findById(approverId);
        String approverName = approver != null ? approver.getRealName() : "系统";
        String result = approveDTO.getApproved() ? "通过" : "拒绝";
        activityService.saveActivity(approveDTO.getDemandId(), approverId, approverName,
                "APPROVE",
                String.format("审批%s了需求%s", result,
                        approveDTO.getComment() != null ? "（意见：" + approveDTO.getComment() + "）" : ""),
                null);
        
        String statusText = approveDTO.getApproved() ? "审批通过" : "已拒绝";
        log.info("需求审批完成: demandId={}, status={}, approver={}", 
                demand.getId(), statusText, approverId);
        
        // 7. 发送通知给提出者
        sendNotification(demand, approverId, approveDTO.getApproved(), approveDTO.getComment());
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
    private void recordStatusChange(Long demandId, Integer oldStatus, Integer newStatus, String remark, Long operatorId) {
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
     * </p>
     *
     * @param demand    需求对象
     * @param approverId 审批人 ID
     * @param approved  是否通过
     * @param comment   审批意见
     */
    private void sendNotification(Demand demand, Long approverId, Boolean approved, String comment) {
        User approver = userMapper.findById(approverId);
        String approverName = approver != null ? approver.getRealName() : "审批人";
        
        String title = approved ? "需求审批通过" : "需求被拒绝";
        String content = String.format(
                "您的需求【%s】已由 %s %s。审批意见：%s",
                demand.getTitle(), approverName, approved ? "审批通过" : "拒绝", comment
        );
        
        notificationService.sendNotification(
                approverId,              // 发送人：审批人
                demand.getProposerId(),  // 接收人：提出者
                title,
                content,
                1,                       // 类型：需求通知
                demand.getId()           // 关联需求 ID
        );
        
        log.debug("已发送审批通知给提出人: demandId={}, proposerId={}", 
                demand.getId(), demand.getProposerId());
    }
}
