package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活动项DTO
 */
@Data
public class ActivityItemDTO {

    @Schema(description = "活动内容")
    private String content;

    @Schema(description = "活动时间")
    private String time;
}
