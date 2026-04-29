package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量移动需求 DTO
 */
@Data
@Schema(description = "批量移动需求请求")
public class BatchMoveDemandDTO {

    @NotEmpty(message = "需求ID列表不能为空")
    @Schema(description = "需求ID列表", required = true)
    private List<Long> ids;

    @NotNull(message = "目标项目ID不能为空")
    @Schema(description = "目标项目ID", required = true)
    private Long targetProjectId;

    @Schema(description = "目标模块ID")
    private Long targetModuleId;

    @Schema(description = "目标迭代ID")
    private Long targetIterationId;
}
