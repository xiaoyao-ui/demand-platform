package com.demand.module.demand.service;

import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.DemandVersionDTO;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandVersion;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.demand.mapper.DemandVersionMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 需求版本管理服务
 * <p>
 * 负责管理需求的版本快照，支持版本追溯和回滚。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandVersionService {

    private final DemandVersionMapper versionMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建版本快照
     * <p>
     * 在需求发生重大变更时调用，保存当前状态的快照。
     * </p>
     *
     * @param demandId 需求 ID
     * @param operatorId 操作人 ID
     * @param changeSummary 变更说明
     * @return 版本号
     */
    @Transactional
    public Integer createVersionSnapshot(Long demandId, Long operatorId, String changeSummary) {
        log.info("创建需求版本快照: demandId={}, operatorId={}", demandId, operatorId);
        
        // 1. 查询需求当前状态
        Demand demand = demandMapper.findById(demandId);
        if (demand == null) {
            throw new BusinessException("需求不存在");
        }
        
        // 2. 获取最新版本号
        Integer latestVersion = versionMapper.getMaxVersionNumber(demandId);
        Integer newVersion = latestVersion + 1;
        
        // 3. 获取操作人信息
        User operator = userMapper.findById(operatorId);
        String operatorName = operator != null ? operator.getRealName() : "系统";
        
        // 4. 创建版本快照
        DemandVersion version = new DemandVersion();
        version.setDemandId(demandId);
        version.setVersionNumber(newVersion);
        version.setTitle(demand.getTitle());
        version.setDescription(demand.getDescription());
        version.setType(demand.getType());
        version.setPriority(demand.getPriority());
        version.setStatus(demand.getStatus());
        version.setModuleId(demand.getModuleId());
        version.setIterationId(demand.getIterationId());
        version.setEstimatedHours(demand.getEstimatedHours());
        version.setStoryPoints(demand.getStoryPoints());
        version.setExpectedStartDate(demand.getExpectedStartDate());
        version.setExpectedEndDate(demand.getExpectedEndDate());
        version.setAcceptanceCriteria(demand.getAcceptanceCriteria());
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);
        version.setOperatorName(operatorName);
        version.setCreateTime(LocalDateTime.now());
        
        // 5. 保存完整快照数据（JSON 格式）
        try {
            Map<String, Object> snapshotData = new HashMap<>();
            snapshotData.put("demandId", demand.getId());
            snapshotData.put("title", demand.getTitle());
            snapshotData.put("version", newVersion);
            snapshotData.put("operator", operatorName);
            snapshotData.put("timestamp", LocalDateTime.now());
            version.setSnapshotData(objectMapper.writeValueAsString(snapshotData));
        } catch (JsonProcessingException e) {
            log.warn("序列化快照数据失败: demandId={}", demandId, e);
            version.setSnapshotData("{}");
        }
        
        // 6. 插入数据库
        versionMapper.insert(version);
        
        log.info("版本快照创建成功: demandId={}, version={}", demandId, newVersion);
        return newVersion;
    }

    /**
     * 查询需求的所有版本
     *
     * @param demandId 需求 ID
     * @return 版本列表
     */
    public List<DemandVersionDTO> getVersionsByDemandId(Long demandId) {
        log.debug("查询需求版本: demandId={}", demandId);
        
        List<DemandVersion> versions = versionMapper.selectByDemandId(demandId);
        
        return versions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 回滚到指定版本
     * <p>
     * 将需求恢复到指定版本的状态，并创建新的版本快照。
     * </p>
     *
     * @param demandId 需求 ID
     * @param targetVersion 目标版本号
     * @param operatorId 操作人 ID
     * @return 新版本号
     */
    @Transactional
    public Integer rollbackToVersion(Long demandId, Integer targetVersion, Long operatorId) {
        log.info("回滚需求版本: demandId={}, targetVersion={}, operatorId={}", 
                demandId, targetVersion, operatorId);
        
        // 1. 查询目标版本
        DemandVersion targetVer = versionMapper.selectByVersion(demandId, targetVersion);
        if (targetVer == null) {
            throw new BusinessException("版本不存在");
        }
        
        // 2. 查询需求当前状态
        Demand demand = demandMapper.findById(demandId);
        if (demand == null) {
            throw new BusinessException("需求不存在");
        }
        
        // 3. 记录变更的字段
        List<String> changedFields = detectChangedFields(demand, targetVer);
        String changeSummary = String.format("回滚到版本 %d，变更字段：%s", 
                targetVersion, String.join(", ", changedFields));
        
        // 4. 先创建当前状态的快照（回滚前）
        createVersionSnapshot(demandId, operatorId, "回滚前快照");
        
        // 5. 恢复需求到目标版本的状态
        demand.setTitle(targetVer.getTitle());
        demand.setDescription(targetVer.getDescription());
        demand.setType(targetVer.getType());
        demand.setPriority(targetVer.getPriority());
        demand.setModuleId(targetVer.getModuleId());
        demand.setIterationId(targetVer.getIterationId());
        demand.setEstimatedHours(targetVer.getEstimatedHours());
        demand.setStoryPoints(targetVer.getStoryPoints());
        demand.setExpectedStartDate(targetVer.getExpectedStartDate());
        demand.setExpectedEndDate(targetVer.getExpectedEndDate());
        demand.setAcceptanceCriteria(targetVer.getAcceptanceCriteria());
        demand.setUpdateTime(LocalDateTime.now());
        
        demandMapper.update(demand);
        
        // 6. 创建回滚后的新版本快照
        Integer newVersion = createVersionSnapshot(demandId, operatorId, changeSummary);
        
        log.info("版本回滚成功: demandId={}, targetVersion={}, newVersion={}", 
                demandId, targetVersion, newVersion);
        
        return newVersion;
    }

    /**
     * 检测变更的字段
     *
     * @param current 当前需求
     * @param target 目标版本
     * @return 变更字段列表
     */
    private List<String> detectChangedFields(Demand current, DemandVersion target) {
        List<String> changed = new ArrayList<>();
        
        if (!Objects.equals(current.getTitle(), target.getTitle())) {
            changed.add("标题");
        }
        if (!Objects.equals(current.getDescription(), target.getDescription())) {
            changed.add("描述");
        }
        if (!Objects.equals(current.getType(), target.getType())) {
            changed.add("类型");
        }
        if (!Objects.equals(current.getPriority(), target.getPriority())) {
            changed.add("优先级");
        }
        if (!Objects.equals(current.getModuleId(), target.getModuleId())) {
            changed.add("模块");
        }
        if (!Objects.equals(current.getEstimatedHours(), target.getEstimatedHours())) {
            changed.add("预估工时");
        }
        if (!Objects.equals(current.getAcceptanceCriteria(), target.getAcceptanceCriteria())) {
            changed.add("验收标准");
        }
        
        return changed.isEmpty() ? Collections.singletonList("未知") : changed;
    }

    /**
     * 转换版本实体为 DTO
     *
     * @param version 版本实体
     * @return 版本 DTO
     */
    private DemandVersionDTO convertToDTO(DemandVersion version) {
        DemandVersionDTO dto = new DemandVersionDTO();
        dto.setId(version.getId());
        dto.setDemandId(version.getDemandId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setTitle(version.getTitle());
        dto.setChangeSummary(version.getChangeSummary());
        dto.setOperatorName(version.getOperatorName());
        dto.setCreateTime(version.getCreateTime());
        
        // 简化显示变更字段
        dto.setChangedFields(version.getChangeSummary());
        
        return dto;
    }
}
