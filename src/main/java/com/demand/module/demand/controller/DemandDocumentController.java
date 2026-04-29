package com.demand.module.demand.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.demand.dto.DocumentCreateDTO;
import com.demand.module.demand.dto.DocumentReviewDTO;
import com.demand.module.demand.entity.DemandDocument;
import com.demand.module.demand.service.DemandDocumentService;
import com.demand.module.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 需求文档管理控制器
 */
@Tag(name = "需求文档管理", description = "需求相关文档的上传、查询、评审")
@RestController
@RequestMapping("/api/demand/{demandId}/documents")
@RequiredArgsConstructor
public class DemandDocumentController {

    private final DemandDocumentService documentService;
    private final PermissionService permissionService;

    /**
     * 上传文档
     */
    @Operation(summary = "上传文档", description = "为需求上传各类文档（PRD、设计稿等）")
    @PostMapping
    @RequirePermission(roles = {1, 2, 3, 4, 5, 6})
    public Result<DemandDocument> uploadDocument(
            @Parameter(description = "需求ID") @PathVariable Long demandId,
            @Valid @RequestBody DocumentCreateDTO dto) {
        Long authorId = permissionService.getCurrentUserId();
        dto.setDemandId(demandId);
        DemandDocument document = documentService.createDocument(dto, authorId);
        return Result.success(document);
    }

    /**
     * 查询所有文档
     */
    @Operation(summary = "查询所有文档", description = "获取需求的所有文档（含历史版本）")
    @GetMapping
    public Result<List<DemandDocument>> getAllDocuments(
            @Parameter(description = "需求ID") @PathVariable Long demandId) {
        List<DemandDocument> documents = documentService.getDocumentsByDemandId(demandId);
        return Result.success(documents);
    }

    /**
     * 查询最新文档
     */
    @Operation(summary = "查询最新文档", description = "获取需求的最新文档（每种类型只返回最新版本）")
    @GetMapping("/latest")
    public Result<List<DemandDocument>> getLatestDocuments(
            @Parameter(description = "需求ID") @PathVariable Long demandId) {
        List<DemandDocument> documents = documentService.getLatestDocuments(demandId);
        return Result.success(documents);
    }

    /**
     * 查看文档
     */
    @Operation(summary = "查看文档", description = "查看文档详情并增加浏览次数")
    @GetMapping("/{documentId}")
    public Result<DemandDocument> viewDocument(
            @Parameter(description = "需求ID") @PathVariable Long demandId,
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        DemandDocument document = documentService.viewDocument(documentId);
        return Result.success(document);
    }

    /**
     * 下载文档
     */
    @Operation(summary = "下载文档", description = "下载文档并增加下载次数")
    @GetMapping("/{documentId}/download")
    public Result<DemandDocument> downloadDocument(
            @Parameter(description = "需求ID") @PathVariable Long demandId,
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        DemandDocument document = documentService.downloadDocument(documentId);
        return Result.success(document);
    }

    /**
     * 评审文档
     */
    @Operation(summary = "评审文档", description = "对文档进行评审（通过或需修改）")
    @PostMapping("/{documentId}/review")
    @RequirePermission(roles = {1, 3, 4})
    public Result<?> reviewDocument(
            @Parameter(description = "需求ID") @PathVariable Long demandId,
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Valid @RequestBody DocumentReviewDTO reviewDTO) {
        Long reviewerId = permissionService.getCurrentUserId();
        documentService.reviewDocument(documentId, reviewDTO, reviewerId);
        return Result.success("评审完成");
    }

    /**
     * 删除文档
     */
    @Operation(summary = "删除文档", description = "删除文档（软删除）")
    @DeleteMapping("/{documentId}")
    @RequirePermission(roles = {1, 2}, requireOwner = true, resourceIdParam = "documentId")
    public Result<?> deleteDocument(
            @Parameter(description = "需求ID") @PathVariable Long demandId,
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        Long operatorId = permissionService.getCurrentUserId();
        documentService.deleteDocument(documentId, operatorId);
        return Result.success("删除成功");
    }
}
