package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文档评审 DTO
 */
@Data
@Schema(description = "文档评审请求")
public class DocumentReviewDTO {

    @NotBlank(message = "评审状态不能为空")
    @Schema(description = "评审状态：APPROVED-通过, REJECTED-需修改",
            example = "APPROVED", required = true)
    private String reviewStatus;

    @Schema(description = "评审意见")
    private String reviewComment;
}
