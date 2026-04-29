package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 项目统计 DTO
 */
@Data
@Schema(description = "项目统计数据")
public class ProjectStatsDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "需求总数")
    private Long totalDemands;

    @Schema(description = "已完成需求数")
    private Long completedDemands;

    @Schema(description = "进行中需求数")
    private Long inProgressDemands;

    @Schema(description = "待审批需求数")
    private Long pendingDemands;

    @Schema(description = "完成率（百分比）")
    private Double completionRate;

    @Schema(description = "平均开发周期（天）")
    private Double avgDevelopmentDays;

    @Schema(description = "状态分布")
    private Map<String, Long> statusDistribution;

    @Schema(description = "类型分布")
    private Map<String, Long> typeDistribution;
}
