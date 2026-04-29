package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量分配 DTO
 */
@Data
@Schema(description = "批量分配请求对象")
public class BatchAssignDTO {

    @NotEmpty(message = "需求ID列表不能为空")
    @Schema(description = "需求ID列表", required = true)
    private List<Long> ids;

    @NotNull(message = "负责人ID不能为空")
    @Schema(description = "负责人ID", required = true)
    private Long assigneeId;
}
