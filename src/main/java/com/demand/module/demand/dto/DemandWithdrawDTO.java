package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 需求撤回请求对象
 */
@Data
@Schema(description = "需求撤回请求对象")
public class DemandWithdrawDTO {

    @Schema(description = "撤回原因", example = "需求描述有误，需要修改")
    private String reason;
}
