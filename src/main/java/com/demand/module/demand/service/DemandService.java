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
import com.demand.module.notification.service.NotificationService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.RoleMapper;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandMapper demandMapper;
    private final DemandStatusHistoryMapper statusHistoryMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DemandActivityService activityService;
    private final DemandTagService demandTagService;
    private final DemandDependencyService demandDependencyService;
    private final com.demand.module.dict.service.DictService dictService;

    /**
     * 创建需求
     */
    @Transactional
    public void createDemand(DemandCreateDTO createDTO, Long creatorId) {
        log.info("创建需求: title={}, creatorId={}", createDTO.getTitle(), creatorId);

        Demand demand = new Demand();
        demand.setTitle(createDTO.getTitle());
        demand.setDescription(createDTO.getDescription());
        demand.setType(createDTO.getType() != null ? createDTO.getType() : "FEATURE");
        demand.setPriority(createDTO.getPriority() != null ? createDTO.getPriority() : "MEDIUM");
        demand.setStatus("DRAFT");
        demand.setProjectId(createDTO.getProjectId());
        demand.setModuleId(createDTO.getModuleId());
        demand.setIterationId(createDTO.getIterationId());
        demand.setCreatorId(creatorId);
        demand.setEstimatedHours(createDTO.getEstimatedHours());
        demand.setStoryPoints(createDTO.getStoryPoints());
        demand.setExpectedStartDate(createDTO.getExpectedStartDate());
        demand.setExpectedEndDate(createDTO.getExpectedEndDate());
        demand.setSource(createDTO.getSource());
        demand.setBusinessValue(createDTO.getBusinessValue());
        demand.setTechnicalSolution(createDTO.getTechnicalSolution());
        demand.setRiskDescription(createDTO.getRiskDescription());
        demand.setAcceptanceCriteria(createDTO.getAcceptanceCriteria());

        demandMapper.insert(demand);

        if (createDTO.getTagIds() != null && !createDTO.getTagIds().isEmpty()) {
            demandTagService.addTagsToDemand(demand.getId(), createDTO.getTagIds());
        }

        User creator = userMapper.findById(creatorId);
        String creatorName = creator != null ? creator.getRealName() : "未知用户";
        activityService.saveActivity(demand.getId(), creatorId, creatorName,
                "CREATE", "创建了需求", null);

        log.info("需求创建成功: demandId={}", demand.getId());
    }

    /**
     * 根据 ID 查询需求详情
     */
    public Demand getDemandById(Long id) {
        log.debug("查询需求详情: demandId={}", id);
        Demand demand = demandMapper.selectDemandWithDetails(id);
        if (demand == null) {
            log.warn("需求不存在: demandId={}", id);
            throw new BusinessException("需求不存在");
        }

        List<String> tags = demandTagService.getTagsByDemandId(id).stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toList());
        demand.setTags(tags);

        return demand;
    }

    /**
     * 更新需求
     */
    @Transactional
    public void updateDemand(Long id, Demand demand, Long operatorId) {
        log.info("更新需求: demandId={}, operatorId={}", id, operatorId);

        // 1. 验证需求是否存在
        Demand existDemand = demandMapper.selectById(id);
        if (existDemand == null) {
            log.warn("需求不存在，更新失败: demandId={}", id);
            throw new BusinessException("需求不存在");
        }

        // 2. 验证状态是否允许编辑（只有草稿或被拒绝的需求可以编辑）
        String currentStatus = existDemand.getStatus();
        if (!"DRAFT".equals(currentStatus) && !"REJECTED".equals(currentStatus)) {
            throw new BusinessException("只有草稿或被拒绝的需求可以编辑");
        }

        // 3. 处理特殊状态的自动填充
        String oldStatus = existDemand.getStatus();
        String newStatus = demand.getStatus();
        
        if ("COMPLETED".equals(newStatus)) {
            demand.setActualEndDate(java.time.LocalDate.now());
            demand.setCompleteTime(LocalDateTime.now());
        }

        // 4. 执行更新
        demand.setId(id);
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);

        // 5. 如果状态发生变更，记录变更历史和操作动态
        if (oldStatus != null && newStatus != null && !oldStatus.equals(newStatus)) {
            handleStatusChange(id, existDemand.getTitle(), oldStatus, newStatus, operatorId);
        }
    }

    /**
     * 处理状态变更的通用逻辑
     */
    private void handleStatusChange(Long demandId, String title, String oldStatus, String newStatus, Long operatorId) {
        log.info("需求状态变更: demandId={}, oldStatus={}, newStatus={}", demandId, oldStatus, newStatus);

        // 1. 记录状态变更历史
        recordStatusChange(demandId, oldStatus, newStatus, null, operatorId);

        // 2. 获取操作人信息
        User operator = userMapper.findById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";

        // 3. 从字典模块获取状态的中文名称
        String oldText = dictService.getDictName("demand_status", oldStatus);
        String newText = dictService.getDictName("demand_status", newStatus);
        
        // 4. 保存操作动态
        String activityContent = String.format("将状态从【%s】修改为【%s】", oldText, newText);
        activityService.saveActivity(demandId, operatorId, operatorName, "STATUS_CHANGE", activityContent, null);

        // 5. 通知提出人（如果操作人不是提出人）
        Demand demand = demandMapper.selectById(demandId);
        if (demand != null && demand.getCreatorId() != null && !demand.getCreatorId().equals(operatorId)) {
            String notificationContent = String.format(
                "您的需求【%s】状态已由 %s 变更为【%s】", 
                title, operatorName, newText
            );
            notificationService.sendNotification(
                operatorId,
                demand.getCreatorId(),
                "需求状态变更",
                notificationContent,
                1,
                demandId
            );
        }
    }

    /**
     * 变更需求状态
     */
    public void changeStatus(Long demandId, String newStatus, String remark, Long operatorId) {
        log.info("变更需求状态: demandId={}, newStatus={}, operatorId={}, remark={}",
                demandId, newStatus, operatorId, remark);

        Demand demand = demandMapper.findById(demandId);
        if (demand == null) {
            log.warn("需求不存在，状态变更失败: demandId={}", demandId);
            throw new BusinessException("需求不存在");
        }

        String oldStatus = demand.getStatus();
        demand.setStatus(newStatus);
        demand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(demand);

        // 处理特殊状态的自动填充
        if ("COMPLETED".equals(newStatus)) {
            demand.setActualEndDate(java.time.LocalDate.now());
            demand.setCompleteTime(LocalDateTime.now());
            demandMapper.update(demand);
        }

        // 统一处理状态变更逻辑
        handleStatusChange(demandId, demand.getTitle(), oldStatus, newStatus, operatorId);
    }

    /**
     * 记录状态变更历史
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
     * 根据需求主键获取需求状态历史
     */
    public List<DemandStatusHistory> getStatusHistory(Long demandId) {
        log.debug("查询需求状态历史: demandId={}", demandId);
        return statusHistoryMapper.findByDemandId(demandId);
    }

    /**
     * 撤回需求
     */
    @Transactional
    public void withdrawDemand(Long id, Long currentUserId, String reason) {
        log.info("撤回需求: demandId={}, currentUserId={}, reason={}", id, currentUserId, reason);
        
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            throw new BusinessException("需求不存在");
        }
        
        if (!existDemand.getCreatorId().equals(currentUserId)) {
            throw new BusinessException("只有需求提出者才能撤回");
        }
        
        if (!"PENDING_REVIEW".equals(existDemand.getStatus())) {
            throw new BusinessException("只能撤回待审批状态的需求");
        }
        
        String oldStatus = existDemand.getStatus();
        existDemand.setStatus("WITHDRAWN");
        existDemand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(existDemand);
        
        recordStatusChange(id, oldStatus, "WITHDRAWN", "撤回原因：" + reason, currentUserId);
        
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
     */
    public void submitForApproval(Long id, Long currentUserId) {
        log.info("提交需求审核: demandId={}, userId={}", id, currentUserId);
        
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            throw new BusinessException("需求不存在");
        }
        
        if (!existDemand.getCreatorId().equals(currentUserId)) {
            throw new BusinessException("只有需求提出者才能提交审核");
        }
        
        if (!"DRAFT".equals(existDemand.getStatus()) && !"WITHDRAWN".equals(existDemand.getStatus())) {
            throw new BusinessException("只有草稿或已撤回状态的需求才能提交审核");
        }
        
        String oldStatus = existDemand.getStatus();
        existDemand.setStatus("PENDING_REVIEW");
        existDemand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(existDemand);
        
        recordStatusChange(id, oldStatus, "PENDING_REVIEW", "提交审核", currentUserId);
        
        // 埋点：记录提交审核
        User operator = userMapper.findById(currentUserId);
        String operatorName = operator != null ? operator.getRealName() : "系统";
        activityService.saveActivity(id, currentUserId, operatorName,
                "SUBMIT", "提交了审核", null);

        List<Long> projectManagerRoleIds = roleMapper.selectRoleIdsByKeys(List.of("PROJECT_MANAGER"));
        if (projectManagerRoleIds != null && !projectManagerRoleIds.isEmpty()) {
            Set<Long> receiverIds = new HashSet<>();
            for (Long roleId : projectManagerRoleIds) {
                List<Long> userIds = roleMapper.selectUserIdsByRoleId(roleId);
                if (userIds != null && !userIds.isEmpty()) {
                    receiverIds.addAll(userIds);
                }
            }

            for (Long receiverId : receiverIds) {
                notificationService.sendNotification(
                        currentUserId,     // userId = 操作人
                        receiverId,        // receiverId = 接收人
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
        if (!existDemand.getCreatorId().equals(currentUserId)) {
            throw new BusinessException("只有需求提出者才能重新提交");
        }
        
        // 3. 验证状态（只能重新提交被拒绝的需求）
        if (!"REJECTED".equals(existDemand.getStatus())) {
            throw new BusinessException("只能重新提交被拒绝状态的需求");
        }
        
        // 4. 更新状态为"待审批"
        String oldStatus = existDemand.getStatus();
        existDemand.setStatus("PENDING_REVIEW"); // 回到待审批状态
        existDemand.setUpdateTime(LocalDateTime.now());
        demandMapper.update(existDemand);
        
        // 5. 记录状态变更历史
        recordStatusChange(id, oldStatus, "PENDING_REVIEW", "重新提交审核", currentUserId);
        
        // 6. 通知所有项目经理
        List<Long> projectManagerRoleIds = roleMapper.selectRoleIdsByKeys(List.of("PROJECT_MANAGER"));
        if (projectManagerRoleIds != null && !projectManagerRoleIds.isEmpty()) {
            Set<Long> receiverIds = new java.util.HashSet<>();
            for (Long roleId : projectManagerRoleIds) {
                List<Long> userIds = roleMapper.selectUserIdsByRoleId(roleId);
                if (userIds != null && !userIds.isEmpty()) {
                    receiverIds.addAll(userIds);
                }
            }

            for (Long receiverId : receiverIds) {
                notificationService.sendNotification(
                        currentUserId,
                        receiverId,
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
     */
    @Transactional
    public void approveDemand(DemandApproveDTO approveDTO, Long approverId) {
        log.info("审批需求: demandId={}, approved={}, comment={}",
                approveDTO.getDemandId(), approveDTO.getApproved(), approveDTO.getComment());

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

        // 5. 记录状态变更历史（包含审批意见）
        recordStatusChange(approveDTO.getDemandId(), oldStatus, newStatus, approveDTO.getComment(), approverId);

        // 6. 获取操作人和状态信息
        User approver = userMapper.findById(approverId);
        String approverName = approver != null ? approver.getRealName() : "系统";
        String resultText = approveDTO.getApproved() ? "通过" : "拒绝";
        String statusText = dictService.getDictName("demand_status", newStatus);

        // 7. 记录操作动态
        String activityContent = approveDTO.getComment() != null && !approveDTO.getComment().isEmpty()
                ? String.format("审批%s了需求（意见：%s）", resultText, approveDTO.getComment())
                : String.format("审批%s了需求", resultText);
        activityService.saveActivity(approveDTO.getDemandId(), approverId, approverName, "APPROVE", activityContent, null);

        // 8. 通知需求提出者
        if (demand.getCreatorId() != null && !demand.getCreatorId().equals(approverId)) {
            String notificationContent = String.format(
                    "您的需求【%s】已%s。审批意见：%s",
                    demand.getTitle(),
                    statusText,
                    approveDTO.getComment() != null ? approveDTO.getComment() : "无"
            );
            notificationService.sendNotification(
                    approverId,              // 发送人：审批人
                    demand.getCreatorId(),   // 接收人：提出者
                    "需求审批结果通知",
                    notificationContent,
                    1,
                    demand.getId()
            );
        }

        log.info("需求审批完成: demandId={}, status={}, approver={}",
                demand.getId(), statusText, approverId);
    }

    /**
     * 删除需求
     */
    public void deleteDemand(Long id, Long currentUserId) {
        log.info("删除需求: demandId={}, currentUserId={}", id, currentUserId);
        
        Demand existDemand = demandMapper.findById(id);
        if (existDemand == null) {
            log.warn("需求不存在，删除失败: demandId={}", id);
            throw new BusinessException("需求不存在");
        }
        
        if (!existDemand.getCreatorId().equals(currentUserId)) {
            log.warn("无权删除需求: demandId={}, proposerId={}, currentUserId={}", 
                    id, existDemand.getCreatorId(), currentUserId);
            throw new BusinessException("只有需求创建者才能删除此需求");
        }
        
        if (!"DRAFT".equals(existDemand.getStatus()) && !"WITHDRAWN".equals(existDemand.getStatus())) {
            throw new BusinessException("只能删除草稿或已撤回状态的需求");
        }
        
        demandMapper.deleteById(id);
        log.info("需求删除成功: demandId={}", id);
    }

    /**
     * 分页查询需求
     */
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
                queryDTO.getCreatorId(),
                queryDTO.getAssigneeId(),
                offset,
                queryDTO.getPageSize()
        );

        Long total = demandMapper.countByCondition(
                queryDTO.getTitle(),
                queryDTO.getType(),
                queryDTO.getPriority(),
                queryDTO.getStatus(),
                queryDTO.getCreatorId(),
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
        
        // 2. 按状态统计（使用字典转义）
        List<Map<String, Object>> statusCounts = demandMapper.countByStatusGroup();
        Map<String, Long> statusMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("status"),
                        m -> (Long) m.get("count")
                ));
        
        // 从字典获取状态名称并设置统计值
        stats.setPendingCount(statusMap.getOrDefault("PENDING_REVIEW", 0L));
        stats.setDevelopingCount(statusMap.getOrDefault("IN_DEVELOPMENT", 0L));
        stats.setCompletedCount(statusMap.getOrDefault("COMPLETED", 0L));
        
        // 3. 类型分布（使用字典转义）
        List<Map<String, Object>> typeCounts = demandMapper.countByTypeGroup();
        Map<String, Long> typeDistribution = new HashMap<>();
        
        // 初始化所有类型为0
        List<com.demand.module.dict.entity.Dict> typeDicts = dictService.getDictByType("demand_type");
        for (com.demand.module.dict.entity.Dict dict : typeDicts) {
            typeDistribution.put(dict.getName(), 0L);
        }
        
        // 填充实际数据
        for (Map<String, Object> row : typeCounts) {
            String typeCode = (String) row.get("type");
            Long count = (Long) row.get("count");
            // 从字典获取中文名称
            String typeName = dictService.getDictName("demand_type", typeCode);
            typeDistribution.put(typeName, count);
        }
        stats.setTypeDistribution(typeDistribution);
        
        // 4. 优先级分布（使用字典转义）
        List<Map<String, Object>> priorityCounts = demandMapper.countByPriorityGroup();
        Map<String, Long> priorityDistribution = new HashMap<>();
        
        // 初始化所有优先级为0
        List<com.demand.module.dict.entity.Dict> priorityDicts = dictService.getDictByType("priority");
        for (com.demand.module.dict.entity.Dict dict : priorityDicts) {
            priorityDistribution.put(dict.getName(), 0L);
        }
        
        // 填充实际数据
        for (Map<String, Object> row : priorityCounts) {
            String priorityCode = (String) row.get("priority");
            Long count = (Long) row.get("count");
            // 从字典获取中文名称
            String priorityName = dictService.getDictName("priority", priorityCode);
            priorityDistribution.put(priorityName, count);
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

    /**
     * 批量分配负责人
     */
    @Transactional
    public void batchAssign(List<Long> ids, Long assigneeId, Long operatorId) {
        if (ids == null || ids.isEmpty()) return;
        User assignee = userMapper.findById(assigneeId);
        String assigneeName = assignee != null ? assignee.getRealName() : "未知用户";
        User operator = userMapper.findById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";

        for (Long id : ids) {
            Demand demand = demandMapper.findById(id);
            if (demand != null && "APPROVED".equals(demand.getStatus())) { // 仅分配审批通过的需求
                String oldStatus = demand.getStatus();
                demand.setAssigneeId(assigneeId);
                demand.setStatus("IN_DEVELOPMENT"); // 批量分配也自动转开发中
                demand.setUpdateTime(LocalDateTime.now());
                demandMapper.update(demand);
                
                recordStatusChange(id, oldStatus, "IN_DEVELOPMENT", "批量分配负责人", operatorId);
                
                activityService.saveActivity(id, operatorId, operatorName,
                        "ASSIGN_STATUS", String.format("批量分配给【%s】并进入开发中", assigneeName), null);
                
                if (demand.getCreatorId() != null) {
                    notificationService.sendNotification(operatorId, demand.getCreatorId(),
                            "需求分配通知",
                            String.format("您的需求【%s】已被分配给 %s", demand.getTitle(), assigneeName), 1, id);
                }
            }
        }
    }

    /**
     * 获取项目统计数据
     *
     * @param projectId 项目ID
     * @return 项目统计信息
     */
    public com.demand.module.demand.dto.ProjectStatsDTO getProjectStats(Long projectId) {
        log.debug("获取项目统计: projectId={}", projectId);
        
        Map<String, Object> stats = demandMapper.getProjectStats(projectId);
        if (stats == null) {
            throw new BusinessException("项目不存在");
        }

        com.demand.module.demand.dto.ProjectStatsDTO dto = new com.demand.module.demand.dto.ProjectStatsDTO();
        dto.setProjectId(projectId);
        
        // 获取项目名称
        var project = com.demand.module.project.mapper.ProjectMapper.class;
        // 这里简化处理，实际应该注入 ProjectMapper
        
        dto.setTotalDemands(((Number) stats.get("totalDemands")).longValue());
        dto.setCompletedDemands(((Number) stats.get("completedDemands")).longValue());
        dto.setInProgressDemands(((Number) stats.get("inProgressDemands")).longValue());
        dto.setPendingDemands(((Number) stats.get("pendingDemands")).longValue());
        dto.setCompletionRate(stats.get("completionRate") != null ? 
                ((Number) stats.get("completionRate")).doubleValue() : 0.0);
        dto.setAvgDevelopmentDays(stats.get("avgDevelopmentDays") != null ? 
                ((Number) stats.get("avgDevelopmentDays")).doubleValue() : 0.0);

        // 获取状态分布
        List<Map<String, Object>> statusDist = demandMapper.getProjectStatusDistribution(projectId);
        Map<String, Long> statusMap = statusDist.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("status"),
                        m -> ((Number) m.get("count")).longValue()
                ));
        dto.setStatusDistribution(statusMap);

        // 获取类型分布
        List<Map<String, Object>> typeDist = demandMapper.getProjectTypeDistribution(projectId);
        Map<String, Long> typeMap = typeDist.stream()
                .collect(Collectors.toMap(
                        m -> dictService.getDictName("demand_type", (String) m.get("type")),
                        m -> ((Number) m.get("count")).longValue()
                ));
        dto.setTypeDistribution(typeMap);

        return dto;
    }

    /**
     * 获取迭代看板数据
     *
     * @param iterationId 迭代ID
     * @return 迭代看板信息
     */
    public com.demand.module.demand.dto.IterationKanbanDTO getIterationKanban(Long iterationId) {
        log.debug("获取迭代看板: iterationId={}", iterationId);
        
        Map<String, Object> kanban = demandMapper.getIterationKanban(iterationId);
        if (kanban == null) {
            throw new BusinessException("迭代不存在");
        }

        com.demand.module.demand.dto.IterationKanbanDTO dto = new com.demand.module.demand.dto.IterationKanbanDTO();
        dto.setIterationId(iterationId);
        dto.setIterationName((String) kanban.get("iterationName"));
        
        if (kanban.get("startDate") != null) {
            dto.setStartDate(((java.sql.Date) kanban.get("startDate")).toLocalDate());
        }
        if (kanban.get("endDate") != null) {
            dto.setEndDate(((java.sql.Date) kanban.get("endDate")).toLocalDate());
        }
        
        dto.setTotalDemands(((Number) kanban.get("totalDemands")).longValue());
        dto.setCompletedCount(((Number) kanban.get("completedCount")).longValue());
        dto.setInProgressCount(((Number) kanban.get("inProgressCount")).longValue());
        dto.setPendingCount(((Number) kanban.get("pendingCount")).longValue());
        dto.setProgressPercent(kanban.get("progressPercent") != null ? 
                ((Number) kanban.get("progressPercent")).doubleValue() : 0.0);

        // 获取状态分布
        List<Map<String, Object>> statusDist = demandMapper.getIterationStatusDistribution(iterationId);
        Map<String, Long> statusMap = statusDist.stream()
                .collect(Collectors.toMap(
                        m -> dictService.getDictName("demand_status", (String) m.get("status")),
                        m -> ((Number) m.get("count")).longValue()
                ));
        dto.setStatusDistribution(statusMap);

        // 获取负责人分布
        List<Map<String, Object>> assigneeDist = demandMapper.getIterationAssigneeDistribution(iterationId);
        Map<String, Long> assigneeMap = assigneeDist.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("assigneeName"),
                        m -> ((Number) m.get("count")).longValue()
                ));
        dto.setAssigneeDistribution(assigneeMap);

        // 获取每日进度趋势（用于燃尽图）
        List<Map<String, Object>> dailyProgress = demandMapper.getIterationDailyProgress(iterationId);
        List<com.demand.module.demand.dto.IterationKanbanDTO.DailyProgressDTO> progressList = 
                buildDailyProgress(dailyProgress, dto.getTotalDemands());
        dto.setDailyProgress(progressList);

        return dto;
    }

    /**
     * 构建每日进度数据（用于燃尽图）
     *
     * @param dailyProgress 每日完成数据
     * @param totalDemands 总需求数
     * @return 每日进度列表
     */
    private List<com.demand.module.demand.dto.IterationKanbanDTO.DailyProgressDTO> buildDailyProgress(
            List<Map<String, Object>> dailyProgress, Long totalDemands) {
        
        List<com.demand.module.demand.dto.IterationKanbanDTO.DailyProgressDTO> result = new java.util.ArrayList<>();
        long cumulativeCompleted = 0;
        
        for (Map<String, Object> day : dailyProgress) {
            cumulativeCompleted += ((Number) day.get("dailyCompleted")).longValue();
            
            com.demand.module.demand.dto.IterationKanbanDTO.DailyProgressDTO dto = 
                    new com.demand.module.demand.dto.IterationKanbanDTO.DailyProgressDTO();
            dto.setDate(((java.sql.Date) day.get("date")).toLocalDate());
            dto.setCumulativeCompleted(cumulativeCompleted);
            dto.setRemainingDemands(totalDemands - cumulativeCompleted);
            
            result.add(dto);
        }
        
        return result;
    }
}
