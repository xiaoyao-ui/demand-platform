package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求查询DTO
 */
@Data
@Schema(description = "需求查询请求对象")
public class DemandQueryDTO {

    @Schema(description = "需求标题", example = "用户管理")
    private String title;

    @Schema(description = "搜索关键词", example = "用户管理")
    private String keyword;
    
    @Schema(description = "需求类型: FEATURE-功能需求, BUG-Bug修复, OPTIMIZATION-优化需求, TASK-任务", example = "FEATURE")
    private String type;
    
    @Schema(description = "优先级: LOW-低, MEDIUM-中, HIGH-高, URGENT-紧急", example = "HIGH")
    private String priority;
    
    @Schema(description = "状态: DRAFT-草稿, PENDING_REVIEW-待审批, APPROVED-审批通过, IN_DEVELOPMENT-开发中等", example = "APPROVED")
    private String status;
    
    @Schema(description = "提出人ID（即创建人ID）", example = "1")
    private Long creatorId;
    
    @Schema(description = "负责人ID", example = "2")
    private Long assigneeId;
    
    @Schema(description = "页码，默认1", example = "1")
    private Integer pageNum = 1;
    
    @Schema(description = "每页数量，默认10", example = "10")
    private Integer pageSize = 10;
}
