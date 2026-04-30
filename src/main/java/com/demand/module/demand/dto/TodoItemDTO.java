package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 待办事项DTO
 */
@Data
public class TodoItemDTO {

    @Schema(description = "待办ID")
    private Long id;

    @Schema(description = "待办类型: approval-审批, task-任务, comment-评论")
    private String type;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "优先级: high-紧急, normal-普通")
    private String priority;

    @Schema(description = "关联ID")
    private Long relatedId;

    @Schema(description = "创建时间")
    private String createTime;
}
