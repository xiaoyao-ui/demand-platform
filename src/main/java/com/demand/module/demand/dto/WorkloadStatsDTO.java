package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工时统计 DTO
 */
@Data
@Schema(description = "工时统计数据")
public class WorkloadStatsDTO {

    @Schema(description = "统计维度：project-项目, user-用户")
    private String dimension;

    @Schema(description = "项目ID（当dimension=project时）")
    private Long projectId;

    @Schema(description = "用户ID（当dimension=user时）")
    private Long userId;

    @Schema(description = "总预估工时（小时）")
    private Double totalEstimatedHours;

    @Schema(description = "总实际工时（小时）")
    private Double totalActualHours;

    @Schema(description = "工时完成率（百分比）")
    private Double completionRate;

    @Schema(description = "需求数量")
    private Long demandCount;

    @Schema(description = "已完成需求数")
    private Long completedDemandCount;

    @Schema(description = "人均工时（小时）")
    private Double avgHoursPerPerson;

    @Schema(description = "按人员分组的工时统计")
    private List<UserWorkloadStats> userStats;

    @Schema(description = "按需求类型分组的工时统计")
    private Map<String, TypeWorkloadStats> typeStats;

    /**
     * 人员工时统计
     */
    @Data
    @Schema(description = "人员工时统计")
    public static class UserWorkloadStats {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户姓名")
        private String userName;

        @Schema(description = "负责需求数")
        private Long demandCount;

        @Schema(description = "预估工时")
        private Double estimatedHours;

        @Schema(description = "实际工时")
        private Double actualHours;

        @Schema(description = "完成率")
        private Double completionRate;
    }

    /**
     * 类型工时统计
     */
    @Data
    @Schema(description = "类型工时统计")
    public static class TypeWorkloadStats {

        @Schema(description = "需求类型")
        private String type;

        @Schema(description = "需求数量")
        private Long count;

        @Schema(description = "预估工时")
        private Double estimatedHours;

        @Schema(description = "实际工时")
        private Double actualHours;
    }
}
