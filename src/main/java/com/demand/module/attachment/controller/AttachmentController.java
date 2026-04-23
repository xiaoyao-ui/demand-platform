package com.demand.module.attachment.controller;

import com.demand.common.Result;
import com.demand.config.PermissionContext;
import com.demand.module.attachment.entity.Attachment;
import com.demand.module.attachment.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 附件管理控制器
 * <p>
 * 提供需求附件的上传、下载、查询和删除功能。
 * 支持多种文件格式（图片、文档、压缩包等），并实现了文件去重和安全防护。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>文件上传</b>：支持多格式文件，自动按日期分目录存储</li>
 *   <li><b>文件下载</b>：安全路径校验，防止路径遍历攻击</li>
 *   <li><b>文件去重</b>：基于 MD5 哈希值检测重复文件，避免重复存储</li>
 *   <li><b>操作审计</b>：上传和删除操作自动记录到需求动态时间轴</li>
 * </ul>
 * 
 * <h3>安全防护：</h3>
 * <ul>
 *   <li><b>文件大小限制</b>：默认最大 10MB，可配置</li>
 *   <li><b>文件类型白名单</b>：仅允许 jpg、png、pdf、docx 等常见格式</li>
 *   <li><b>路径遍历防护</b>：下载时校验文件路径，防止访问系统其他文件</li>
 *   <li><b>文件名随机化</b>：使用 UUID 重命名，防止文件名冲突和注入攻击</li>
 * </ul>
 * 
 * <h3>存储结构示例：</h3>
 * <pre>
 * uploads/
 * └── 2026/
 *     └── 04/
 *         └── 23/
 *             └── a1b2c3d4e5f6.pdf
 * </pre>
 */
@Tag(name = "附件管理", description = "需求附件的上传、下载、删除接口")
@RestController
@RequestMapping("/api/attachment")
@RequiredArgsConstructor
public class AttachmentController {

    /**
     * 附件业务逻辑服务
     */
    private final AttachmentService attachmentService;

    /**
     * 权限上下文，用于获取当前登录用户 ID
     */
    private final PermissionContext permissionContext;

    /**
     * 上传附件
     * <p>
     * 为指定需求上传附件文件，支持多种格式。
     * 上传成功后会自动记录到需求的动态时间轴。
     * </p>
     * 
     * <p>
     * <b>上传流程</b>：
     * <ol>
     *   <li>验证用户登录状态</li>
     *   <li>校验文件大小和类型</li>
     *   <li>计算文件 MD5 哈希值，检查是否已存在（去重）</li>
     *   <li>生成存储路径：{@code yyyy/MM/dd/uuid.ext}</li>
     *   <li>保存文件并插入数据库记录</li>
     *   <li>记录操作动态："用户上传了附件 xxx.pdf"</li>
     * </ol>
     * </p>
     *
     * @param demandId 需求 ID
     * @param file     上传的文件
     * @param request  HTTP 请求对象（备用，当前使用 PermissionContext 获取 userId）
     * @return 附件信息（包含 ID、文件名、路径、大小等）
     * @throws BusinessException 当文件为空、类型不支持或大小超限时抛出
     */
    @Operation(summary = "上传附件", description = "为需求上传附件文件")
    @PostMapping("/upload/{demandId}")
    public Result<Attachment> uploadFile(@Parameter(description = "需求ID") @PathVariable Long demandId,
                                         @Parameter(description = "附件文件") @RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) {
        Long uploaderId = permissionContext.getUserId();
        if (uploaderId == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        Attachment attachment = attachmentService.uploadFile(demandId, file, uploaderId);
        return Result.success(attachment);
    }

    /**
     * 查询需求的附件列表
     * <p>
     * 返回指定需求的所有附件，按上传时间倒序排列。
     * 前端可用于展示附件列表，支持点击下载或删除。
     * </p>
     *
     * @param demandId 需求 ID
     * @return 附件列表
     */
    @Operation(summary = "查询附件列表", description = "查询指定需求的所有附件")
    @GetMapping("/demand/{demandId}")
    public Result<List<Attachment>> getAttachments(@Parameter(description = "需求ID") @PathVariable Long demandId) {
        List<Attachment> attachments = attachmentService.getAttachmentsByDemandId(demandId);
        return Result.success(attachments);
    }

    /**
     * 下载附件
     * <p>
     * 根据附件 ID 下载文件，支持所有文件格式。
     * 下载时会进行严格的路径校验，防止路径遍历攻击。
     * </p>
     * 
     * <p>
     * <b>安全机制</b>：
     * <ul>
     *   <li>清理路径中的 {@code ../} 序列，防止目录穿越</li>
     *   <li>校验最终路径是否在上传目录内</li>
     *   <li>检查文件是否存在且可读</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>响应头</b>：
     * <ul>
     *   <li>{@code Content-Type: application/octet-stream} - 二进制流</li>
     *   <li>{@code Content-Disposition: attachment; filename="xxx.pdf"} - 触发浏览器下载</li>
     * </ul>
     * </p>
     *
     * @param id 附件 ID
     * @return 文件资源（二进制流）
     * @throws MalformedURLException 当文件路径格式错误时抛出
     * @throws RuntimeException      当文件不存在或路径非法时抛出
     */
    @Operation(summary = "下载附件", description = "下载指定的附件文件")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@Parameter(description = "附件ID") @PathVariable Long id) throws MalformedURLException {
        // 1. 查询附件信息
        Attachment attachment = attachmentService.getAttachmentById(id);
        
        // 2. 构建安全的文件路径
        String absolutePath = attachmentService.getAbsoluteUploadPath();
        String sanitizedPath = attachment.getFilePath().replaceAll("\\.\\./", "");
        Path filePath = Paths.get(absolutePath, sanitizedPath).normalize();
        
        // 3. 校验路径安全性（防止路径遍历攻击）
        Path uploadDir = Paths.get(absolutePath).normalize();
        if (!filePath.startsWith(uploadDir)) {
            throw new RuntimeException("非法的文件路径");
        }
        
        Resource resource = new UrlResource(filePath.toUri());

        // 4. 检查文件是否存在且可读
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("文件不存在或不可读");
        }

        // 5. 返回文件流
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    /**
     * 删除附件
     * <p>
     * 删除指定的附件，包括数据库记录和物理文件。
     * 删除操作会记录到需求的动态时间轴。
     * </p>
     * 
     * <p>
     * <b>删除流程</b>：
     * <ol>
     *   <li>查询附件信息（用于记录动态）</li>
     *   <li>记录操作动态："用户删除了附件 xxx.pdf"</li>
     *   <li>删除物理文件</li>
     *   <li>删除数据库记录</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>注意</b>：删除操作不可恢复，请谨慎使用！
     * </p>
     *
     * @param id 附件 ID
     * @return 操作结果
     * @throws BusinessException 当附件不存在时抛出
     */
    @Operation(summary = "删除附件", description = "删除指定的附件")
    @DeleteMapping("/{id}")
    public Result<?> deleteAttachment(@Parameter(description = "附件ID") @PathVariable Long id) {
        attachmentService.deleteAttachment(id);
        return Result.success();
    }
}
