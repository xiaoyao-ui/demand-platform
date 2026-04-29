package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 迭代看板 DTO
 */
@Data
@Schema(description = "迭代看板数据")
public class IterationKanbanDTO {

    @Schema(description = "迭代ID")
    private Long iterationId;

    @Schema(description = "迭代名称")
    private String iterationName;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "需求总数")
    private Long totalDemands;

    @Schema(description = "已完成数")
    private Long completedCount;

    @Schema(description = "进行中数")
    private Long inProgressCount;

    @Schema(description = "待处理数")
    private Long pendingCount;

    @Schema(description = "进度百分比")
    private Double progressPercent;

    @Schema(description = "状态分布")
    private Map<String, Long> statusDistribution;

    @Schema(description = "负责人分布")
    private Map<String, Long> assigneeDistribution;

    @Schema(description = "每日完成趋势（用于燃尽图）")
    private List<DailyProgressDTO> dailyProgress;

    /**
     * 每日进度 DTO
     */
    @Data
    @Schema(description = "每日进度数据")
    public static class DailyProgressDTO {
        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "累计完成数")
        private Long cumulativeCompleted;

        @Schema(description = "剩余需求数")
        private Long remainingDemands;
    }
}
