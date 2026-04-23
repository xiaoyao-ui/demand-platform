package com.demand.module.attachment.service;

import com.demand.exception.BusinessException;
import com.demand.module.attachment.entity.Attachment;
import com.demand.module.attachment.mapper.AttachmentMapper;
import com.demand.module.demand.service.DemandActivityService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 附件业务逻辑服务
 * <p>
 * 负责附件的上传、下载、删除等操作。
 * 集成文件去重、类型校验、大小限制、操作审计等功能。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>文件上传</b>：支持多格式文件，自动按日期分目录存储</li>
 *   <li><b>文件去重</b>：基于 MD5 哈希值检测重复文件，避免重复存储</li>
 *   <li><b>操作审计</b>：上传和删除操作自动记录到需求动态时间轴</li>
 *   <li><b>安全防护</b>：文件类型白名单、大小限制、路径校验</li>
 * </ul>
 * 
 * <h3>配置项：</h3>
 * <ul>
 *   <li>{@code file.upload.path} - 文件上传根路径（默认 ./uploads）</li>
 *   <li>{@code file.upload.allowed-types} - 允许的文件类型（默认 jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar）</li>
 *   <li>{@code file.upload.max-size} - 最大文件大小（默认 10MB）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    /**
     * 附件数据访问层
     */
    private final AttachmentMapper attachmentMapper;

    /**
     * 需求动态服务，用于记录附件操作
     */
    private final DemandActivityService activityService;

    /**
     * 用户数据访问层，用于查询上传者姓名
     */
    private final UserMapper userMapper;

    /**
     * 文件上传根路径
     * <p>
     * 从 application.properties 读取，默认为 {@code ./uploads}
     * </p>
     */
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 允许的文件类型（逗号分隔）
     * <p>
     * 例如："jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar"
     * </p>
     */
    @Value("${file.upload.allowed-types:jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar}")
    private String allowedTypesStr;

    /**
     * 最大文件大小（字节）
     * <p>
     * 默认 10MB（10485760 字节）
     * </p>
     */
    @Value("${file.upload.max-size:10485760}")
    private Long maxFileSize;

    /**
     * 获取绝对上传路径
     * <p>
     * 将相对路径（如 ./uploads）转换为绝对路径，兼容 Windows 和 Linux。
     * </p>
     *
     * @return 绝对路径
     */
    public String getAbsoluteUploadPath() {
        if (uploadPath.startsWith("./") || uploadPath.startsWith(".\\")) {
            return new java.io.File(uploadPath).getAbsolutePath();
        }
        return uploadPath;
    }

    /**
     * 上传附件
     * <p>
     * 完整的文件上传流程，包含校验、去重、存储和审计。
     * </p>
     * 
     * <p>
     * <b>上传流程</b>：
     * <ol>
     *   <li>校验文件非空、大小不超过限制、类型在白名单内</li>
     *   <li>计算文件 MD5 哈希值，检查是否已存在（去重）</li>
     *   <li>如果文件已存在，直接复用记录并关联到新需求</li>
     *   <li>生成存储路径：{@code yyyy/MM/dd/uuid.ext}</li>
     *   <li>创建目录并保存文件到磁盘</li>
     *   <li>插入数据库记录</li>
     *   <li>记录操作动态："用户上传了附件 xxx.pdf"</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>文件去重</b>：
     * 如果两个用户上传了内容完全相同的文件（MD5 相同），系统只会存储一份物理文件，
     * 但会在数据库中创建两条记录，分别关联到不同的需求。
     * </p>
     *
     * @param demandId   需求 ID
     * @param file       上传的文件
     * @param uploaderId 上传者 ID
     * @return 附件信息
     * @throws BusinessException 当文件为空、类型不支持、大小超限或 IO 异常时抛出
     */
    public Attachment uploadFile(Long demandId, MultipartFile file, Long uploaderId) {
        log.info("上传附件: demandId={}, fileName={}, fileSize={}, uploaderId={}", 
                demandId, file.getOriginalFilename(), file.getSize(), uploaderId);
        
        // 1. 校验文件非空
        if (file.isEmpty()) {
            log.warn("文件上传失败: 文件为空");
            throw new BusinessException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.warn("文件上传失败: 文件名无效");
            throw new BusinessException("文件名无效");
        }

        // 2. 校验文件大小
        long fileSize = file.getSize();
        if (fileSize > maxFileSize) {
            throw new BusinessException("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 3. 校验文件类型
        String fileType = getFileExtension(originalFilename);
        if (!isAllowedType(fileType)) {
            throw new BusinessException("不支持的文件类型: " + fileType + 
                    "，支持的类型: " + allowedTypesStr);
        }

        // 4. 计算文件 MD5 哈希值，检查是否已存在（去重）
        String fileHash = calculateFileHash(file);
        Attachment existAttachment = attachmentMapper.findByFileHash(fileHash);
        if (existAttachment != null) {
            log.info("文件已存在，跳过上传: hash={}", fileHash);
            existAttachment.setDemandId(demandId);
            return existAttachment;
        }

        // 5. 生成存储路径：yyyy/MM/dd/uuid.ext
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + fileType;
        String relativePath = dateFolder + "/" + fileName;
        
        String absolutePath = getAbsoluteUploadPath();
        String fullPath = absolutePath + "/" + relativePath;

        // 6. 创建目录并保存文件
        try {
            File destFile = new File(fullPath);
            if (!destFile.getParentFile().exists()) {
                boolean created = destFile.getParentFile().mkdirs();
                if (!created) {
                    log.error("创建目录失败: {}", destFile.getParentFile().getAbsolutePath());
                    throw new BusinessException("创建上传目录失败");
                }
            }
            file.transferTo(destFile);
            log.info("文件保存成功: path={}", fullPath);
        } catch (IOException e) {
            log.error("文件上传失败: fileName={}, error={}", originalFilename, e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        // 7. 插入数据库记录
        Attachment attachment = new Attachment();
        attachment.setDemandId(demandId);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(relativePath);
        attachment.setFileType(fileType);
        attachment.setFileSize(fileSize);
        attachment.setUploaderId(uploaderId);
        attachment.setFileHash(fileHash);
        attachment.setCreateTime(LocalDateTime.now());

        attachmentMapper.insert(attachment);
        
        // 8. 记录操作动态
        User uploader = userMapper.findById(uploaderId);
        String uploaderName = uploader != null ? uploader.getRealName() : "系统";
        activityService.saveActivity(demandId, uploaderId, uploaderName,
                "ATTACHMENT",
                String.format("上传了附件 %s", originalFilename),
                null);
        
        log.info("附件上传成功: attachmentId={}, fileName={}", attachment.getId(), originalFilename);
        return attachment;
    }

    /**
     * 查询需求的所有附件
     *
     * @param demandId 需求 ID
     * @return 附件列表
     */
    public List<Attachment> getAttachmentsByDemandId(Long demandId) {
        log.debug("查询需求附件: demandId={}", demandId);
        return attachmentMapper.findByDemandId(demandId);
    }

    /**
     * 根据 ID 查询附件
     *
     * @param id 附件 ID
     * @return 附件对象
     * @throws BusinessException 当附件不存在时抛出
     */
    public Attachment getAttachmentById(Long id) {
        log.debug("查询附件详情: attachmentId={}", id);
        Attachment attachment = attachmentMapper.findById(id);
        if (attachment == null) {
            log.warn("附件不存在: attachmentId={}", id);
            throw new BusinessException("附件不存在");
        }
        return attachment;
    }

    /**
     * 删除附件
     * <p>
     * 删除附件的数据库记录和物理文件，并记录操作动态。
     * </p>
     * 
     * <p>
     * <b>删除流程</b>：
     * <ol>
     *   <li>查询附件信息（用于记录动态）</li>
     *   <li>记录操作动态："用户删除了附件 xxx.pdf"（必须在删数据库前拿到附件信息）</li>
     *   <li>删除物理文件</li>
     *   <li>删除数据库记录</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>注意</b>：如果物理文件已被手动删除，只删除数据库记录，不报错。
     * </p>
     *
     * @param id 附件 ID
     * @throws BusinessException 当附件不存在时抛出
     */
    public void deleteAttachment(Long id) {
        log.info("删除附件: attachmentId={}", id);
        
        // 1. 查询附件信息
        Attachment attachment = attachmentMapper.findById(id);
        if (attachment == null) {
            log.warn("附件不存在，删除失败: attachmentId={}", id);
            throw new BusinessException("附件不存在");
        }

        // 2. 记录操作动态（必须在删数据库前拿到附件信息）
        User deleter = userMapper.findById(attachment.getUploaderId());
        String deleterName = deleter != null ? deleter.getRealName() : "系统";
        activityService.saveActivity(attachment.getDemandId(), attachment.getUploaderId(), deleterName,
                "ATTACHMENT_DELETE",
                String.format("删除了附件 %s", attachment.getFileName()),
                null);

        // 3. 删除物理文件
        String absolutePath = getAbsoluteUploadPath();
        String fullPath = absolutePath + "/" + attachment.getFilePath();
        File file = new File(fullPath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("删除物理文件: path={}", fullPath);
            } else {
                log.warn("删除物理文件失败: path={}", fullPath);
            }
        } else {
            log.warn("物理文件不存在: path={}", fullPath);
        }

        // 4. 删除数据库记录
        attachmentMapper.deleteById(id);
        log.info("附件删除成功: attachmentId={}", id);
    }

    /**
     * 获取文件扩展名
     * <p>
     * 从文件名中提取扩展名，并转换为小写。
     * 例如："document.PDF" → "pdf"
     * </p>
     *
     * @param fileName 文件名
     * @return 扩展名（小写），如果没有扩展名则返回空字符串
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 检查文件类型是否允许
     * <p>
     * 从配置文件中读取允许的文件类型列表，判断当前文件类型是否在白名单内。
     * </p>
     *
     * @param fileType 文件扩展名
     * @return true 表示允许，false 表示禁止
     */
    private boolean isAllowedType(String fileType) {
        List<String> allowedTypes = Arrays.asList(allowedTypesStr.split(","));
        return allowedTypes.contains(fileType.toLowerCase());
    }

    /**
     * 计算文件 MD5 哈希值
     * <p>
     * 使用 MD5 算法计算文件的哈希值，用于文件去重和完整性校验。
     * 相同内容的文件具有相同的 MD5 值，即使文件名不同。
     * </p>
     * 
     * <p>
     * <b>示例</b>：{@code d41d8cd98f00b204e9800998ecf8427e}
     * </p>
     *
     * @param file 上传的文件
     * @return MD5 哈希值（32位十六进制字符串），如果计算失败则返回随机 UUID
     */
    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算文件哈希失败: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }
}
