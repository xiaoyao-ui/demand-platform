package com.demand.module.demand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求实体类
 */
@Data
@TableName("demand")
public class Demand {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 需求标题
     */
    private String title;

    /**
     * 需求详细描述
     */
    private String description;

    /**
     * 需求类型
     */
    private String type;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 状态
     */
    private String status;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 所属模块ID
     */
    private Long moduleId;

    /**
     * 所属迭代ID
     */
    private Long iterationId;

    /**
     * 父需求ID
     */
    private Long parentId;

    /**
     * 提出人ID
     */
    private Long creatorId;

    /**
     * 当前负责人ID
     */
    private Long assigneeId;

    /**
     * 评审人/验收人ID
     */
    private Long reviewerId;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 预估工时
     */
    private BigDecimal estimatedHours;

    /**
     * 实际工时
     */
    private BigDecimal actualHours;

    /**
     * 故事点
     */
    private Integer storyPoints;

    /**
     * 期望开始日期
     */
    private LocalDate expectedStartDate;

    /**
     * 期望完成日期
     */
    private LocalDate expectedEndDate;

    /**
     * 实际开始日期
     */
    private LocalDate actualStartDate;

    /**
     * 实际完成日期
     */
    private LocalDate actualEndDate;

    /**
     * 审批时间
     */
    private LocalDateTime approveTime;

    /**
     * 开始开发时间
     */
    private LocalDateTime startDevelopTime;

    /**
     * 开始测试时间
     */
    private LocalDateTime startTestTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 审批意见
     */
    private String approveComment;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 验收标准
     */
    private String acceptanceCriteria;

    /**
     * 验收结果
     */
    private String acceptanceResult;

    /**
     * 版本号
     */
    private String version;

    /**
     * 需求来源
     */
    private String source;

    /**
     * 业务价值说明
     */
    private String businessValue;

    /**
     * 技术方案概要
     */
    private String technicalSolution;

    /**
     * 风险说明
     */
    private String riskDescription;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 负责人名称
     */
    private String assigneeName;

    /**
     * 评审人名称
     */
    private String reviewerName;

    /**
     * 审批人名称
     */
    private String approverName;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 迭代名称
     */
    private String iterationName;

    /**
     * 标签集合
     */
    private List<String> tags;
}
