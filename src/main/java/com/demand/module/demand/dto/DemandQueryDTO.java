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
    
    @Schema(description = "需求类型: 0-功能需求, 1-优化需求, 2-Bug修复", example = "0")
    private Integer type;
    
    @Schema(description = "优先级: 0-低, 1-中, 2-高, 3-紧急", example = "2")
    private Integer priority;
    
    @Schema(description = "状态: 0-待评审, 1-已通过, 2-开发中, 3-测试中, 4-已完成, 5-已拒绝", example = "1")
    private Integer status;
    
    @Schema(description = "提出人ID", example = "1")
    private Long proposerId;
    
    @Schema(description = "负责人ID", example = "2")
    private Long assigneeId;
    
    @Schema(description = "页码，默认1", example = "1")
    private Integer pageNum = 1;
    
    @Schema(description = "每页数量，默认10", example = "10")
    private Integer pageSize = 10;
}
