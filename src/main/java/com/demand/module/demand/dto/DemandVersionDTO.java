package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 需求版本 DTO
 */
@Data
@Schema(description = "需求版本信息")
public class DemandVersionDTO {

    @Schema(description = "版本ID")
    private Long id;

    @Schema(description = "需求ID")
    private Long demandId;

    @Schema(description = "版本号")
    private Integer versionNumber;

    @Schema(description = "需求标题")
    private String title;

    @Schema(description = "变更说明")
    private String changeSummary;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "主要变更字段")
    private String changedFields;
}
