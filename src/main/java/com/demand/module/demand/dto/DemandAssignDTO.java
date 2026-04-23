package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 需求分配请求对象
 */
@Data
@Schema(description = "需求分配请求对象")
public class DemandAssignDTO {

    @NotNull(message = "需求ID不能为空")
    @Schema(description = "需求ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long demandId;

    @NotNull(message = "负责人ID不能为空")
    @Schema(description = "负责人ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assigneeId;
}
