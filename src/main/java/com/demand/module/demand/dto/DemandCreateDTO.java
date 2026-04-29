package com.demand.module.demand.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "需求创建/更新请求对象")
//自动忽略未定义的字段
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandCreateDTO {

    @NotBlank(message = "需求标题不能为空")
    @Size(max = 200, message = "需求标题长度不能超过 200")
    @Schema(description = "需求标题", example = "新增用户管理功能", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 10000, message = "需求描述长度不能超过 10000")
    @Schema(description = "需求详细描述（支持Markdown）", example = "需要实现用户的增删改查功能，包括...")
    private String description;

    @Schema(description = "需求类型：FEATURE-功能需求, BUG-Bug修复, OPTIMIZATION-优化需求, TASK-任务", example = "FEATURE")
    private String type;

    @Schema(description = "优先级：URGENT-紧急, HIGH-高, MEDIUM-中, LOW-低", example = "MEDIUM")
    private String priority;

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "所属项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "所属模块ID", example = "5")
    private Long moduleId;

    @Schema(description = "所属迭代ID", example = "2")
    private Long iterationId;

    @Schema(description = "父需求ID（支持层级结构）", example = "0")
    private Long parentId;

    @Schema(description = "预估工时（小时）", example = "40.0")
    private BigDecimal estimatedHours;

    @Schema(description = "故事点（敏捷开发）", example = "8")
    private Integer storyPoints;

    @Schema(description = "期望开始日期", example = "2026-04-01")
    private LocalDate expectedStartDate;

    @Schema(description = "期望完成日期", example = "2026-04-30")
    private LocalDate expectedEndDate;

    @Schema(description = "需求来源：用户反馈/市场调研/内部优化等", example = "用户反馈")
    private String source;

    @Size(max = 2000, message = "业务价值说明长度不能超过 2000")
    @Schema(description = "业务价值说明", example = "提升用户体验，减少操作步骤")
    private String businessValue;

    @Size(max = 2000, message = "技术方案概要长度不能超过 2000")
    @Schema(description = "技术方案概要", example = "采用微服务架构，使用Spring Cloud...")
    private String technicalSolution;

    @Size(max = 2000, message = "风险说明长度不能超过 2000")
    @Schema(description = "风险说明", example = "可能存在性能瓶颈，需要进行压力测试")
    private String riskDescription;

    @Size(max = 2000, message = "验收标准长度不能超过 2000")
    @Schema(description = "验收标准", example = "- [ ] 功能正常\n- [ ] 性能达标\n- [ ] 通过测试")
    private String acceptanceCriteria;

    @Schema(description = "标签ID列表", example = "[1, 2, 5]")
    private List<Long> tagIds;

    @Schema(description = "依赖的需求ID列表", example = "[10, 15]")
    private List<Long> dependencyIds;
}
