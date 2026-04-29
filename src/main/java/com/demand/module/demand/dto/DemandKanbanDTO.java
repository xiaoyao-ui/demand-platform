package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 需求看板 DTO
 * <p>
 * 用于支持看板视图展示，按状态分组显示需求。
 * </p>
 */
@Data
@Schema(description = "需求看板数据")
public class DemandKanbanDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "看板列（按状态分组）")
    private List<KanbanColumn> columns;

    /**
     * 看板列
     */
    @Data
    @Schema(description = "看板列")
    public static class KanbanColumn {

        @Schema(description = "状态码")
        private String status;

        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "该状态下的需求列表")
        private List<KanbanCard> cards;

        @Schema(description = "需求数量")
        private Integer count;
    }

    /**
     * 看板卡片
     */
    @Data
    @Schema(description = "看板卡片")
    public static class KanbanCard {

        @Schema(description = "需求ID")
        private Long id;

        @Schema(description = "需求标题")
        private String title;

        @Schema(description = "优先级")
        private String priority;

        @Schema(description = "负责人ID")
        private Long assigneeId;

        @Schema(description = "负责人姓名")
        private String assigneeName;

        @Schema(description = "创建人姓名")
        private String creatorName;

        @Schema(description = "期望完成日期")
        private java.time.LocalDate expectedEndDate;

        @Schema(description = "故事点")
        private Integer storyPoints;
    }
}
