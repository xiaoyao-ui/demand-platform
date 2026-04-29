package com.demand.module.demand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 需求版本实体类
 * <p>
 * 记录需求的每次重大变更快照，支持版本追溯和回滚。
 * </p>
 */
@Data
@TableName("demand_version")
public class DemandVersion {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 需求 ID
     */
    private Long demandId;

    /**
     * 版本号（从 1 开始递增）
     */
    private Integer versionNumber;

    /**
     * 需求标题快照
     */
    private String title;

    /**
     * 需求描述快照
     */
    private String description;

    /**
     * 需求类型快照
     */
    private String type;

    /**
     * 优先级快照
     */
    private String priority;

    /**
     * 状态快照
     */
    private String status;

    /**
     * 模块 ID 快照
     */
    private Long moduleId;

    /**
     * 迭代 ID 快照
     */
    private Long iterationId;

    /**
     * 预估工时快照
     */
    private BigDecimal estimatedHours;

    /**
     * 故事点快照
     */
    private Integer storyPoints;

    /**
     * 期望开始日期快照
     */
    private LocalDate expectedStartDate;

    /**
     * 期望完成日期快照
     */
    private LocalDate expectedEndDate;

    /**
     * 验收标准快照
     */
    private String acceptanceCriteria;

    /**
     * 完整快照数据（JSON 格式）
     * <p>
     * 存储需求的完整信息，便于未来扩展
     * </p>
     */
    private String snapshotData;

    /**
     * 变更说明
     */
    private String changeSummary;

    /**
     * 操作人 ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
