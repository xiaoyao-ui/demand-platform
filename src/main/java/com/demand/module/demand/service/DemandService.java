package com.demand.module.demand.service;

import com.demand.common.PageResult;
import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.DashboardStats;
import com.demand.module.demand.dto.DemandApproveDTO;
import com.demand.module.demand.dto.DemandCreateDTO;
import com.demand.module.demand.dto.DemandQueryDTO;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandStatusHistory;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.demand.mapper.DemandStatusHistoryMapper;
import com.demand.module.demand.service.DemandActivityService;
import com.demand.module.notification.service.NotificationService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 需求业务逻辑服务
 * <p>
 * 负责需求的完整生命周期管理，包括创建、更新、删除、状态变更、审批等。
 * 集成操作审计、状态历史记录、实时通知等功能。
 * </p>
 *
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>需求CRUD</b>：创建、查询、更新、删除需求</li>
 *   <li><b>状态管理</b>：提交审核、撤回、重新提交、状态变更</li>
 *   <li><b>操作审计</b>：记录所有关键操作到时间轴</li>
 *   <li><b>状态历史</b>：记录每次状态变更的详细信息</li>
 *   <li><b>实时通知</b>：状态变更时通知相关人员</li>
 *   <li><b>数据统计</b>：提供仪表盘统计数据</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandMapper demandMapper;
    private final DemandStatusHistoryMapper statusHistoryMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final DemandActivityService activityService;

    /**
     * 创建需求
     * <p>
     * 新建需求后状态默认为"待审批"（status=0）。
     * 同时记录操作动态："创建了需求"。
     * </p>
     *
     * @param createDTO 需求创建对象
     */
    public void createDemand(DemandCreateDTO createDTO) {
        log.info("创建需求: title={}, type={}, proposerId={}",
                createDTO.getTitle(), createDTO.getType(), createDTO.getProposerId());

        Demand demand = new Demand();
        demand.setTitle(createDTO.getTitle());
        demand.setDescription(createDTO.getDescription());
        demand.setType(createDTO.getType());
        demand.setPriority(createDTO.getPriority());
        demand.setStatus(0);
        demand.setProposerId(createDTO.getProposerId());
        demand.setModule(createDTO.getModule());
        demand.setExpectedDate(createDTO.getExpectedDate());
        demand.setCreateTime(LocalDateTime.now());
        demand.setUpdateTime(LocalDateTime.now());

        demandMapper.insert(demand);
        
        // 埋点：记录需求创建
        User proposer = userMapper.findById(createDTO.getProposerId());
        String proposerName = proposer != null ? proposer.getRealName() : "未知用户";
        activityService.saveActivity(demand.getId(), createDTO.getProposerId(), proposerName,
                "CREATE", "创建了需求", null);
        
        log.info("需求创建成功: demandId={}, title={}", demand.getId(), demand.getTitle());
    }

    /**
     * 根据 ID 查询需求详情
     *
     * @param id 需求 ID
     * @return 需求对象（包含提出人、负责人、审批人姓名）
     * @throws BusinessException 当需求不存在时抛出
     */
    public Demand getDemandById(Long id) {
        log.debug("查询需求详情: demandId={}", id);
        Demand demand = demandMapper.findById(id);
        if (demand == null) {
            log.warn("需求不存在: demandId={}", id);
            throw new BusinessException("需求不存在");
        }
        return demand;
    }

    /**
     * 更新需求
     * <p>
     * 支持部分字段更新（动态 SQL）。
     * 如果更新了状态字段，会自动：
     * <ol>
     *   <li>记录状态变更历史</li>
     *   <li>记录操作动态</li>
     *   <li>发送通知给提出人</li>
     * </ol>
     * </p>
     *
     * @param id         需求 ID
     * @param demand     更新对象
     * @param operatorId 操作人 ID
     */
    @Transactional
    public void updateDemand(Long id, Demand demand, Long operatorId) {
        log.info("更新需求: demandId={}, operatorId={}", id, operatorId);

        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            log.warn("需求不存在，更新失败: demandId={}", id);
            throw new BusinessException("需求不存在");
        }

        Integer oldStatus = existDemand.getStatus();
        Integer newStatus = demand.getStatus();

        // 如果状态变为已完成，自动记录实际完成时间
        if (newStatus != null && newStatus == 4) {
            demand.setActualDate(LocalDateTime.now());
        }

        demand.setId(id);
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);

        if (oldStatus != null && newStatus != null && !oldStatus.equals(newStatus)) {
            log.info("需求状态变更: demandId={}, oldStatus={}, newStatus={}", id, oldStatus, newStatus);
            recordStatusChange(id, oldStatus, newStatus, null, operatorId);

            // 核心修复：补充操作动态埋点
            User operator = userMapper.findById(operatorId);
            String operatorName = operator != null ? operator.getRealName() : "系统";
            String oldText = getStatusText(oldStatus);
            String newText = getStatusText(newStatus);
            activityService.saveActivity(id, operatorId, operatorName,
                    "STATUS_CHANGE",
                    String.format("将状态从【%s】修改为【%s】", oldText, newText),
                    null);

            if (existDemand.getProposerId() != null && !existDemand.getProposerId().equals(operatorId)) {
                User operatorForNotif = userMapper.findById(operatorId);
                String operatorNameForNotif = operatorForNotif != null ? operatorForNotif.getRealName() : "系统";
                notificationService.sendNotification(
                        operatorId,
                        existDemand.getProposerId(),
                        "需求状态变更",
                        "您的需求【" + existDemand.getTitle() + "】状态已由 " + operatorNameForNotif + " 变更为【" + getStatusText(newStatus) + "】",
                        1,
                        id
                );
            }
        }
    }

    /**
     * 变更需求状态
     * <p>
     * 通用的状态变更方法，适用于各种场景。
     * 会自动记录状态历史、操作动态，并通知相关人员。
     * </p>
     *
     * @param demandId   需求 ID
     * @param newStatus  新状态
     * @param remark     备注说明
     * @param operatorId 操作人 ID
     */
    public void changeStatus(Long demandId, Integer newStatus, String remark, Long operatorId) {
        log.info("变更需求状态: demandId={}, newStatus={}, operatorId={}, remark={}",
                demandId, newStatus, operatorId, remark);

        Demand demand = demandMapper.findById(demandId);
        if (demand == null) {
            log.warn("需求不存在，状态变更失败: demandId={}", demandId);
            throw new BusinessException("需求不存在");
        }

        Integer oldStatus = demand.getStatus();
        demand.setStatus(newStatus);
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);

        recordStatusChange(demandId, oldStatus, newStatus, remark, operatorId);

        // 埋点：记录状态变更
        User operator = userMapper.findById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";
        String oldText = getStatusText(oldStatus);
        String newText = getStatusText(newStatus);
        activityService.saveActivity(demandId, operatorId, operatorName,
                "STATUS_CHANGE",
                String.format("将状态从【%s】修改为【%s】", oldText, newText),
                null);

        String statusText = getStatusText(newStatus);

        //通知提出人需求变更
        if (demand.getProposerId() != null && !demand.getProposerId().equals(operatorId)) {
            notificationService.sendNotification(
                    operatorId,             // userId = 操作人
                    demand.getProposerId(), // receiverId = 接收人（提出人）
                    "需求状态变更",
                    "您的需求【" + demand.getTitle() + "】状态已变更为: " + statusText + " by " + operatorName,
                    1,
                    demandId
            );
        }

        //通知分配人需求变更
        if (demand.getAssigneeId() != null && !demand.getAssigneeId().equals(operatorId)) {
            notificationService.sendNotification(
                    operatorId,             // userId = 操作人
                    demand.getAssigneeId(), // receiverId = 接收人（分配人）
                    "分配的需求状态变更",
                    "您负责的需求【" + demand.getTitle() + "】状态已变更为: " + statusText + " by " + operatorName,
                    1,
                    demandId
            );
        }
    }

    //需求状态代码转义
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待审批";
            case 1:
                return "审批通过";
            case 2:
                return "开发中";
            case 3:
                return "测试中";
            case 4:
                return "已完成";
            case 5:
                return "已拒绝";
            default:
                return "未知";
        }
    }

    //新增状态变更记录
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

    //根据需求主键获取需求状态历史
    public List<DemandStatusHistory> getStatusHistory(Long demandId) {
        log.debug("查询需求状态历史: demandId={}", demandId);
        return statusHistoryMapper.findByDemandId(demandId);
    }

    /**
     * 撤回需求
     * <p>
     * 提出人可以将待审批的需求撤回，状态变回"草稿"。
     * 需要填写撤回原因。
     * </p>
     *
     * @param id            需求 ID
     * @param currentUserId 当前用户 ID
     * @param reason        撤回原因
     * @throws BusinessException 当用户无权限或状态不正确时抛出
     */
    @Transactional
    public void withdrawDemand(Long id, Long currentUserId, String reason) {
        log.info("撤回需求: demandId={}, currentUserId={}, reason={}", id, currentUserId, reason);
        
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            throw new BusinessException("需求不存在");
        }
        
        if (!existDemand.getProposerId().equals(currentUserId)) {
            throw new BusinessException("只有需求提出者才能撤回");
        }
        
        if (existDemand.getStatus() != 0) {
            throw new BusinessException("只能撤回待审批状态的需求");
        }
        
        Integer oldStatus = existDemand.getStatus();
        existDemand.setStatus(6);
        existDemand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(existDemand);
        
        recordStatusChange(id, oldStatus, 6, "撤回原因：" + reason, currentUserId);
        
        // 埋点：记录需求撤回
        User operator = userMapper.findById(currentUserId);
        String operatorName = operator != null ? operator.getRealName() : "系统";
        activityService.saveActivity(id, currentUserId, operatorName,
                "WITHDRAW", "撤回了需求（返回草稿）", null);

        // 通知分配人（如果已分配）
        if (existDemand.getAssigneeId() != null) {
            notificationService.sendNotification(
                    currentUserId,            // userId = 操作人
                    existDemand.getAssigneeId(), // receiverId = 接收人
                    "需求已撤回",
                    String.format("您负责的需求【%s】已被提出人撤回，已返回草稿状态。原因：%s", existDemand.getTitle(), reason),
                    1,
                    id
            );
        }
        
        log.info("需求撤回成功: demandId={}", id);
    }

    /**
     * 提交需求审核
     * <p>
     * 将草稿状态的需求提交审核，状态变为"待审批"。
     * 同时通知所有项目经理和管理员。
     * </p>
     *
     * @param id            需求 ID
     * @param currentUserId 当前用户 ID
     * @throws BusinessException 当用户无权限或状态不正确时抛出
     */
    public void submitForApproval(Long id, Long currentUserId) {
        log.info("提交需求审核: demandId={}, currentUserId={}", id, currentUserId);
        
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            throw new BusinessException("需求不存在");
        }
        
        if (!existDemand.getProposerId().equals(currentUserId)) {
            throw new BusinessException("只有需求提出者才能提交审核");
        }
        
        if (existDemand.getStatus() != 6) {
            throw new BusinessException("只有草稿状态的需求才能提交审核");
        }
        
        Integer oldStatus = existDemand.getStatus();
        existDemand.setStatus(0);
        existDemand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(existDemand);
        
        recordStatusChange(id, oldStatus, 0, "提交审核", currentUserId);
        
        // 埋点：记录提交审核
        User operator = userMapper.findById(currentUserId);
        String operatorName = operator != null ? operator.getRealName() : "系统";
        activityService.saveActivity(id, currentUserId, operatorName,
                "SUBMIT", "提交了审核", null);

        List<User> managers = userMapper.findByRole(3);
        if (managers != null) {
            for (User manager : managers) {
                notificationService.sendNotification(
                        currentUserId,     // userId = 操作人
                        manager.getId(),   // receiverId = 接收人
                        "新需求待审批",
                        String.format("需求【%s】已提交审核，请及时处理。", existDemand.getTitle()),
                        1,
                        id
                );
            }
        }
    }

    /**
     * 重新提交被拒绝的需求
     * <p>
     * 当需求被项目经理拒绝后，提出人可以修改需求内容并重新提交审核。
     * 状态从"已拒绝"(5) 变回"待审批"(0)，进入新一轮审批流程。
     * </p>
     * 
     * <p>
     * <b>业务流程</b>：
     * <ol>
     *   <li>验证需求是否存在</li>
     *   <li>验证当前用户是否为需求提出者</li>
     *   <li>验证需求状态是否为"已拒绝"(5)</li>
     *   <li>将状态改为"待审批"(0)</li>
     *   <li>记录状态变更历史</li>
     *   <li>通知所有项目经理重新审批</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>需求被拒绝后，提出人根据审批意见修改了需求描述</li>
     *   <li>补充了缺失的流程图或原型图</li>
     *   <li>调整了优先级或期望完成时间</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>通知机制</b>：
     * 系统会自动向所有项目经理发送通知："需求【XXX】已修改并重新提交审核，请再次处理。"
     * </p>
     *
     * @param id            需求 ID
     * @param currentUserId 当前用户 ID（必须是需求提出者）
     * @throws BusinessException 当需求不存在、用户无权限或状态不正确时抛出
     */
    @Transactional
    public void resubmitDemand(Long id, Long currentUserId) {
        log.info("重新提交需求: demandId={}, currentUserId={}", id, currentUserId);
        
        // 1. 验证需求是否存在
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            throw new BusinessException("需求不存在");
        }
        
        // 2. 验证权限（只有提出者可以重新提交）
        if (!existDemand.getProposerId().equals(currentUserId)) {
            throw new BusinessException("只有需求提出者才能重新提交");
        }
        
        // 3. 验证状态（只能重新提交被拒绝的需求）
        if (existDemand.getStatus() != 5) {
            throw new BusinessException("只能重新提交被拒绝状态的需求");
        }
        
        // 4. 更新状态为"待审批"
        Integer oldStatus = existDemand.getStatus();
        existDemand.setStatus(0); // 回到待审批状态
        existDemand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(existDemand);
        
        // 5. 记录状态变更历史
        recordStatusChange(id, oldStatus, 0, "重新提交审核", currentUserId);
        
        // 6. 通知所有项目经理
        List<User> managers = userMapper.findByRole(3);
        if (managers != null) {
            for (User manager : managers) {
                notificationService.sendNotification(
                        currentUserId,
                        manager.getId(),
                        "需求重新提交",
                        String.format("需求【%s】已修改并重新提交审核，请再次处理。", existDemand.getTitle()),
                        1,
                        id
                );
            }
        }
        
        log.info("需求重新提交成功: demandId={}", id);
    }

    /**
     * 审批需求
     * <p>
     * 项目经理或管理员对待审批的需求进行审批，可以选择通过或拒绝。
     * 审批通过后状态变为"审批通过"(1)，拒绝后变为"已拒绝"(5)。
     * </p>
     * 
     * <p>
     * <b>业务流程</b>：
     * <ol>
     *   <li>验证需求是否存在</li>
     *   <li>根据审批结果更新状态（通过→1，拒绝→5）</li>
     *   <li>记录审批时间和审批意见</li>
     *   <li>记录状态变更历史</li>
     *   <li>记录操作动态（包含审批意见）</li>
     *   <li>发送通知给需求提出者</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>审批结果</b>：
     * <ul>
     *   <li><b>通过</b>：状态变为"审批通过"(1)，需求可以进入开发阶段</li>
     *   <li><b>拒绝</b>：状态变为"已拒绝"(5)，提出人可以修改后重新提交</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>审批意见</b>：
     * <ul>
     *   <li>选填字段，但建议填写具体的审批意见</li>
     *   <li>通过时可以填写："需求描述清晰，同意开发"</li>
     *   <li>拒绝时必须填写原因："需求描述不够详细，请补充流程图"</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>通知示例</b>：
     * <ul>
     *   <li>通过："您的需求【用户登录功能】已通过。审批意见：需求描述清晰，同意开发"</li>
     *   <li>拒绝："您的需求【用户登录功能】已拒绝。审批意见：缺少详细的接口文档"</li>
     * </ul>
     * </p>
     *
     * @param approveDTO  审批对象（包含需求ID、是否通过、审批意见）
     * @param approverId  审批人 ID（项目经理或管理员）
     * @throws BusinessException 当需求不存在时抛出
     */
    public void approveDemand(DemandApproveDTO approveDTO, Long approverId) {
        log.info("审批需求: demandId={}, approved={}, comment={}", 
                approveDTO.getDemandId(), approveDTO.getApproved(), approveDTO.getComment());
        
        // 1. 验证需求是否存在
        Demand demand = demandMapper.findById(approveDTO.getDemandId());
        if (demand == null) {
            throw new BusinessException("需求不存在");
        }
        
        // 2. 根据审批结果更新状态
        Integer oldStatus = demand.getStatus();
        demand.setStatus(approveDTO.getApproved() ? 1 : 5); // 通过→1，拒绝→5
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);
        
        // 3. 记录状态变更历史（包含审批意见）
        recordStatusChange(approveDTO.getDemandId(), oldStatus, demand.getStatus(), approveDTO.getComment(), approverId);
        
        // 4. 记录操作动态
        User approver = userMapper.findById(approverId);
        String approverName = approver != null ? approver.getRealName() : "系统";
        String result = approveDTO.getApproved() ? "通过" : "拒绝";
        activityService.saveActivity(approveDTO.getDemandId(), approverId, approverName,
                "APPROVE",
                String.format("审批%s了需求%s", result, approveDTO.getComment() != null ? "（意见：" + approveDTO.getComment() + "）" : ""),
                null);

        // 5. 通知需求提出者
        if (demand.getProposerId() != null) {
            notificationService.sendNotification(
                    approverId,              // 发送人：审批人
                    demand.getProposerId(),  // 接收人：提出者
                    "需求审批结果通知",
                    String.format("您的需求【%s】已%s。审批意见：%s", 
                            demand.getTitle(), 
                            approveDTO.getApproved() ? "通过" : "拒绝",
                            approveDTO.getComment()),
                    1,
                    demand.getId()
            );
        }
    }

    /**
     * 删除需求
     * <p>
     * 只能删除草稿状态的需求，防止误删已流转的需求。
     * </p>
     *
     * @param id            需求 ID
     * @param currentUserId 当前用户 ID
     * @throws BusinessException 当用户无权限或状态不正确时抛出
     */
    public void deleteDemand(Long id, Long currentUserId) {
        log.info("删除需求: demandId={}, currentUserId={}", id, currentUserId);
        
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            log.warn("需求不存在，删除失败: demandId={}", id);
            throw new BusinessException("需求不存在");
        }
        
        if (!existDemand.getProposerId().equals(currentUserId)) {
            log.warn("无权删除需求: demandId={}, proposerId={}, currentUserId={}", 
                    id, existDemand.getProposerId(), currentUserId);
            throw new BusinessException("只有需求创建者才能删除此需求");
        }
        
        if (existDemand.getStatus() != 6) {
            throw new BusinessException("只能删除草稿状态的需求");
        }
        
        demandMapper.deleteById(id);
        log.info("需求删除成功: demandId={}", id);
    }

    //分页查询需求
    public PageResult<Demand> queryDemands(DemandQueryDTO queryDTO) {
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            queryDTO.setPageSize(10);
        }
        if (queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(100);
        }
        
        log.debug("分页查询需求: title={}, status={}, pageNum={}, pageSize={}", 
                queryDTO.getTitle(), queryDTO.getStatus(), queryDTO.getPageNum(), queryDTO.getPageSize());
        
        Integer offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();
        List<Demand> list = demandMapper.findByCondition(
                queryDTO.getTitle(),
                queryDTO.getType(),
                queryDTO.getPriority(),
                queryDTO.getStatus(),
                queryDTO.getProposerId(),
                queryDTO.getAssigneeId(),
                offset,
                queryDTO.getPageSize()
        );

        Long total = demandMapper.countByCondition(
                queryDTO.getTitle(),
                queryDTO.getType(),
                queryDTO.getPriority(),
                queryDTO.getStatus(),
                queryDTO.getProposerId(),
                queryDTO.getAssigneeId()
        );

        log.debug("查询需求结果: total={}", total);
        return new PageResult<>(list, total, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    /**
     * 获取仪表盘统计数据
     * <p>
     * 使用 Spring Cache 缓存统计结果，减少数据库压力。
     * 返回数据包括：
     * <ul>
     *   <li>总需求数、待审批数、开发中数、已完成数</li>
     *   <li>类型分布（功能需求、优化需求、Bug修复）</li>
     *   <li>优先级分布（低、中、高、紧急）</li>
     *   <li>近7天趋势数据</li>
     * </ul>
     * </p>
     *
     * @return 仪表盘统计数据
     */
    @Cacheable(value = "dashboardStats", key = "'all'")
    public DashboardStats getDashboardStats() {
        log.debug("获取仪表盘统计数据");
        
        DashboardStats stats = new DashboardStats();
        
        // 1. 统计总数
        Long total = demandMapper.countByCondition(null, null, null, null, null, null);
        stats.setTotalDemand(total);
        
        // 2. 按状态统计
        List<Map<String, Object>> statusCounts = demandMapper.countByStatus();
        Map<Integer, Long> statusMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        m -> (Integer) m.get("status"),
                        m -> (Long) m.get("count")
                ));
        stats.setPendingCount(statusMap.getOrDefault(0, 0L));
        stats.setDevelopingCount(statusMap.getOrDefault(2, 0L));
        stats.setCompletedCount(statusMap.getOrDefault(4, 0L));
        
        // 3. 类型分布
        List<Map<String, Object>> typeCounts = demandMapper.countByType();
        Map<String, Long> typeDistribution = new HashMap<>();
        typeDistribution.put("功能需求", 0L);
        typeDistribution.put("优化需求", 0L);
        typeDistribution.put("Bug修复", 0L);
        for (Map<String, Object> row : typeCounts) {
            Integer type = (Integer) row.get("type");
            Long count = (Long) row.get("count");
            String label = switch (type) {
                case 0 -> "功能需求";
                case 1 -> "优化需求";
                case 2 -> "Bug修复";
                default -> "其他";
            };
            typeDistribution.put(label, count);
        }
        stats.setTypeDistribution(typeDistribution);
        
        // 4. 优先级分布
        List<Map<String, Object>> priorityCounts = demandMapper.countByPriority();
        Map<String, Long> priorityDistribution = new HashMap<>();
        priorityDistribution.put("低", 0L);
        priorityDistribution.put("中", 0L);
        priorityDistribution.put("高", 0L);
        priorityDistribution.put("紧急", 0L);
        for (Map<String, Object> row : priorityCounts) {
            Integer priority = (Integer) row.get("priority");
            Long count = (Long) row.get("count");
            String label = switch (priority) {
                case 0 -> "低";
                case 1 -> "中";
                case 2 -> "高";
                case 3 -> "紧急";
                default -> "未知";
            };
            priorityDistribution.put(label, count);
        }
        stats.setPriorityDistribution(priorityDistribution);
        
        // 5. 近7天趋势
        List<Map<String, Object>> dateCounts = demandMapper.countByDateForLast7Days();
        Map<String, Long> trendData = new HashMap<>();
        // 初始化近7天的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            trendData.put(date.toString(), 0L);
        }
        // 填充实际数据
        for (Map<String, Object> row : dateCounts) {
            String date = row.get("date").toString();
            Long count = (Long) row.get("count");
            trendData.put(date, count);
        }
        stats.setTrendData(trendData);
        
        return stats;
    }

    // 批量分配负责人
    @Transactional
    public void batchAssign(List<Long> ids, Long assigneeId, Long operatorId) {
        if (ids == null || ids.isEmpty()) return;
        User assignee = userMapper.findById(assigneeId);
        String assigneeName = assignee != null ? assignee.getRealName() : "未知用户";
        User operator = userMapper.findById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";

        for (Long id : ids) {
            Demand demand = demandMapper.findById(id);
            if (demand != null && demand.getStatus() == 1) { // 仅分配审批通过的需求
                Integer oldStatus = demand.getStatus();
                demand.setAssigneeId(assigneeId);
                demand.setStatus(2); // 批量分配也自动转开发中
                demand.setUpdateTime(LocalDateTime.now());
                demandMapper.update(demand);
                
                recordStatusChange(id, oldStatus, 2, "批量分配负责人", operatorId);
                
                activityService.saveActivity(id, operatorId, operatorName,
                        "ASSIGN_STATUS", String.format("批量分配给【%s】并进入开发中", assigneeName), null);
                
                if (demand.getProposerId() != null) {
                    notificationService.sendNotification(operatorId, demand.getProposerId(),
                            "需求分配通知",
                            String.format("您的需求【%s】已被分配给 %s", demand.getTitle(), assigneeName), 1, id);
                }
            }
        }
    }
}
