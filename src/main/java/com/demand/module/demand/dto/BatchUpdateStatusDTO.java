package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量更新状态 DTO
 */
@Data
@Schema(description = "批量更新状态请求")
public class BatchUpdateStatusDTO {

    @NotEmpty(message = "需求ID列表不能为空")
    @Schema(description = "需求ID列表", required = true)
    private List<Long> ids;

    @NotEmpty(message = "状态不能为空")
    @Schema(description = "目标状态", example = "APPROVED", required = true)
    private String status;

    @Schema(description = "变更原因/备注")
    private String reason;
}
