package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量添加标签 DTO
 */
@Data
@Schema(description = "批量添加标签请求")
public class BatchAddTagsDTO {

    @NotEmpty(message = "需求ID列表不能为空")
    @Schema(description = "需求ID列表", required = true)
    private List<Long> ids;

    @NotEmpty(message = "标签ID列表不能为空")
    @Schema(description = "标签ID列表", required = true)
    private List<Long> tagIds;
}
