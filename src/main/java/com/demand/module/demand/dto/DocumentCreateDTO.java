package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档创建/更新 DTO
 */
@Data
@Schema(description = "文档创建/更新请求")
public class DocumentCreateDTO {

    @NotNull(message = "需求ID不能为空")
    @Schema(description = "需求ID", required = true)
    private Long demandId;

    @NotBlank(message = "文档类型不能为空")
    @Schema(description = "文档类型：PRD, UI_DESIGN, API_DOC, TECH_DESIGN, TEST_CASE, USER_MANUAL, OTHER",
            example = "PRD", required = true)
    private String docType;

    @NotBlank(message = "文档标题不能为空")
    @Schema(description = "文档标题", example = "产品需求文档V1.0", required = true)
    private String title;

    @Schema(description = "文档描述")
    private String description;

    @Schema(description = "文件URL（在线文档链接或附件地址）")
    private String fileUrl;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件类型：pdf, docx, xlsx, pptx, md, link")
    private String fileType;

    @Schema(description = "版本说明")
    private String versionNote;

    @Schema(description = "访问级别：PUBLIC, INTERNAL, CONFIDENTIAL", example = "PUBLIC")
    private String accessLevel;
}
