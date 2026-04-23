package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 需求审批请求对象
 */
@Data
@Schema(description = "需求审批请求对象")
public class DemandApproveDTO {

    @NotNull(message = "需求ID不能为空")
    @Schema(description = "需求ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long demandId;

    @NotNull(message = "审批结果不能为空")
    @Schema(description = "审批结果: true-通过, false-拒绝", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;

    @Schema(description = "审批意见", example = "需求合理，同意开发", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comment;
}