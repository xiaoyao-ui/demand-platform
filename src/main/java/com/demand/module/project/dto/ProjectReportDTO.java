package com.demand.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 项目报告 DTO（周报/月报通用）
 */
@Data
@Schema(description = "项目报告数据")
public class ProjectReportDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "报告类型：WEEKLY-周报, MONTHLY-月报")
    private String reportType;

    @Schema(description = "报告周期开始日期")
    private LocalDate startDate;

    @Schema(description = "报告周期结束日期")
    private LocalDate endDate;

    @Schema(description = "生成时间")
    private java.time.LocalDateTime generateTime;

    @Schema(description = "总体统计")
    private ReportSummary summary;

    @Schema(description = "需求列表")
    private List<ReportDemand> demands;

    @Schema(description = "成员工作量")
    private List<MemberWorkload> memberWorkloads;

    @Schema(description = "状态分布")
    private Map<String, Integer> statusDistribution;

    @Schema(description = "类型分布")
    private Map<String, Integer> typeDistribution;

    @Schema(description = "本周/本月完成的需求")
    private List<ReportDemand> completedDemands;

    @Schema(description = "进行中的需求")
    private List<ReportDemand> inProgressDemands;

    @Schema(description = "新增需求")
    private List<ReportDemand> newDemands;

    /**
     * 总体统计
     */
    @Data
    @Schema(description = "报告总体统计")
    public static class ReportSummary {
        @Schema(description = "需求总数")
        private Integer totalDemands;

        @Schema(description = "已完成数")
        private Integer completedCount;

        @Schema(description = "进行中数")
        private Integer inProgressCount;

        @Schema(description = "新增数")
        private Integer newCount;

        @Schema(description = "完成率")
        private Double completionRate;

        @Schema(description = "总工时")
        private Double totalHours;

        @Schema(description = "人均工时")
        private Double avgHoursPerPerson;
    }

    /**
     * 报告中的需求
     */
    @Data
    @Schema(description = "报告需求项")
    public static class ReportDemand {
        @Schema(description = "需求ID")
        private Long id;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "类型")
        private String type;

        @Schema(description = "优先级")
        private String priority;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "负责人")
        private String assigneeName;

        @Schema(description = "创建时间")
        private LocalDate createTime;

        @Schema(description = "完成时间")
        private LocalDate completeTime;

        @Schema(description = "预估工时")
        private Double estimatedHours;

        @Schema(description = "实际工时")
        private Double actualHours;
    }

    /**
     * 成员工作量
     */
    @Data
    @Schema(description = "成员工作量")
    public static class MemberWorkload {
        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "姓名")
        private String userName;

        @Schema(description = "负责需求数")
        private Integer demandCount;

        @Schema(description = "完成需求数")
        private Integer completedCount;

        @Schema(description = "预估工时")
        private Double estimatedHours;

        @Schema(description = "实际工时")
        private Double actualHours;

        @Schema(description = "完成率")
        private Double completionRate;
    }
}
