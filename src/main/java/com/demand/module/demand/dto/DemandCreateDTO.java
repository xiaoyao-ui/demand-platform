package com.demand.module.demand.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "需求创建/更新请求对象")
//自动忽略未定义的字段
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandCreateDTO {
    
    @Schema(description = "需求 ID（更新时必填，创建时不需要）")
    private Long id;
    
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 200, message = "需求标题长度不能超过 200")
    @Schema(description = "需求标题", example = "新增用户管理功能", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    
    @Size(max = 5000, message = "需求描述长度不能超过 5000")
    @Schema(description = "需求描述", example = "需要实现用户的增删改查功能")
    private String description;
    
    @NotNull(message = "需求类型不能为空")
    @Schema(description = "需求类型：0-功能需求，1-优化需求，2-Bug修复", example = "0")
    private Integer type;
    
    @NotNull(message = "优先级不能为空")
    @Schema(description = "优先级：0-低，1-中，2-高，3-紧急", example = "2")
    private Integer priority;
    
    @Size(max = 100, message = "模块名称长度不能超过 100")
    @Schema(description = "所属模块", example = "用户管理")
    private String module;
    
    @Schema(description = "期望完成日期", example = "2026-05-17T10:00:00")
    private LocalDateTime expectedDate;
    
    @Schema(description = "需求状态（状态变更时使用）")
    private Integer status;
    
    @Schema(description = "分配负责人 ID（分配需求时使用）")
    private Long assigneeId;
    
    @Schema(description = "审批人 ID（审批需求时使用）")
    private Long approverId;
    
    @Schema(description = "审批时间")
    private LocalDateTime approveTime;
    
    @Schema(description = "审批意见")
    private String approveComment;
    
    @Schema(description = "实际完成时间")
    private LocalDateTime actualDate;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
    
    @Schema(description = "提出人姓名")
    private String proposerName;
    
    @Schema(description = "负责人姓名")
    private String assigneeName;
    
    @Schema(description = "审批人姓名")
    private String approverName;
    
    @Schema(description = "提出人 ID（由系统自动设置，无需传入）", hidden = true)
    private Long proposerId;
}
