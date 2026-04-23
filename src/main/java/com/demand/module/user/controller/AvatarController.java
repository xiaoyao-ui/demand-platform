package com.demand.module.user.controller;

import com.demand.common.Result;
import com.demand.config.PermissionContext;
import com.demand.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 头像管理控制器
 * <p>
 * 提供用户头像上传功能，支持多种图片格式（JPG、PNG、GIF、WebP）。
 * 文件按日期分目录存储，避免单目录文件过多影响性能。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>文件验证</b>：检查文件类型、大小（最大 2MB）、扩展名</li>
 *   <li><b>路径管理</b>：按 {@code yyyy/MM/dd} 格式创建子目录</li>
 *   <li><b>文件名生成</b>：使用 UUID 防止文件名冲突</li>
 *   <li><b>数据库更新</b>：上传成功后自动更新用户头像字段</li>
 * </ul>
 * 
 * <h3>存储结构示例：</h3>
 * <pre>
 * uploads/
 * └── avatar/
 *     └── 2026/
 *         └── 04/
 *             └── 23/
 *                 └── a1b2c3d4e5f6.jpg
 * </pre>
 * 
 * <p>
 * <b>访问方式</b>：{@code http://localhost:8080/avatar/2026/04/23/a1b2c3d4e5f6.jpg}
 * </p>
 */
@Slf4j
@Tag(name = "头像管理", description = "用户头像上传和删除接口")
@RestController
@RequestMapping("/api/user/avatar")
@RequiredArgsConstructor
public class AvatarController {

    /**
     * 用户业务逻辑服务
     */
    private final UserService userService;

    /**
     * 权限上下文，用于获取当前登录用户 ID
     */
    private final PermissionContext permissionContext;

    /**
     * 文件上传根路径（从 application.properties 读取）
     */
    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 上传用户头像
     * <p>
     * 处理流程：
     * 1. 验证用户登录状态
     * 2. 校验文件有效性（非空、图片类型、大小 ≤ 2MB）
     * 3. 验证文件扩展名（仅允许 jpg、jpeg、png、gif、webp）
     * 4. 生成存储路径：{@code ${uploadPath}/avatar/yyyy/MM/dd/uuid.ext}
     * 5. 保存文件并更新数据库中的头像 URL
     * </p>
     * 
     * <p>
     * <b>安全限制</b>：
     * <ul>
     *   <li>文件大小：最大 2MB，防止占用过多存储空间</li>
     *   <li>文件类型：仅允许图片格式，阻止可执行文件上传</li>
     *   <li>文件名：使用 UUID 重命名，防止路径遍历攻击</li>
     * </ul>
     * </p>
     *
     * @param file 上传的图片文件
     * @return 包含头像访问 URL 的 Map 对象
     */
    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 1. 验证用户登录
        Long userId = permissionContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        // 2. 验证文件非空
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }

        // 3. 验证文件类型（必须是图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }

        // 4. 验证文件大小（最大 2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过 2MB");
        }

        try {
            // 5. 提取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 6. 验证文件扩展名白名单
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
            boolean isValidExtension = false;
            for (String ext : allowedExtensions) {
                if (ext.equalsIgnoreCase(extension)) {
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                return Result.error(400, "只支持 jpg、jpeg、png、gif、webp 格式的图片");
            }

            // 7. 生成存储路径（按日期分目录）
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            
            // 8. 将相对路径转换为绝对路径（兼容 Windows/Linux）
            String absolutePath = uploadPath;
            if (absolutePath.startsWith("./") || absolutePath.startsWith(".\\")) {
                absolutePath = new File(absolutePath).getAbsolutePath();
            }
            
            Path uploadDir = Paths.get(absolutePath, "avatar", dateDir);
            Files.createDirectories(uploadDir);

            // 9. 保存文件到磁盘
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            // 10. 更新用户头像 URL（使用独立静态资源前缀 /avatar/）
            String avatarUrl = "/avatar/" + dateDir + "/" + fileName;
            userService.updateUserAvatar(userId, avatarUrl);

            log.info("头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);

            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", avatarUrl);
            return Result.success(result);

        } catch (IOException e) {
            log.error("头像上传失败: userId={}", userId, e);
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }
    }
}
