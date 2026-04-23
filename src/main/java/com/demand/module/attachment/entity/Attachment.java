package com.demand.module.attachment.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 附件实体类
 * <p>
 * 对应数据库 {@code attachment} 表，存储需求相关的附件文件信息。
 * 支持文件去重（基于 MD5 哈希值）和操作审计。
 * </p>
 * 
 * <h3>典型数据示例：</h3>
 * <pre>
 * +----+-----------+------------------+---------------------+-----------+------------+--------------+----------------------------------+---------------------+
 * | id | demand_id | file_name        | file_path           | file_type | file_size  | uploader_id  | file_hash                        | create_time         |
 * +----+-----------+------------------+---------------------+-----------+------------+--------------+----------------------------------+---------------------+
 * |  1 |       100 | 需求文档.pdf      | 2026/04/23/abc.pdf  | pdf       | 1048576    |           10 | d41d8cd98f00b204e9800998ecf8427e | 2026-04-23 10:30:00 |
 * |  2 |       100 | 原型图.png        | 2026/04/23/def.png  | png       | 524288     |           10 | e99a18c428cb38d5f260853678922e03 | 2026-04-23 11:00:00 |
 * +----+-----------+------------------+---------------------+-----------+------------+--------------+----------------------------------+---------------------+
 * </pre>
 * 
 * <h3>文件去重机制：</h3>
 * <p>
 * 通过 {@code file_hash} 字段实现文件去重：
 * <ul>
 *   <li>上传前计算文件的 MD5 哈希值</li>
 *   <li>如果数据库中已存在相同哈希值的文件，直接复用记录</li>
 *   <li>避免重复存储相同内容的文件，节省磁盘空间</li>
 * </ul>
 * </p>
 */
@Data
public class Attachment {

    /**
     * 附件 ID（主键）
     */
    private Long id;

    /**
     * 需求 ID（外键）
     * <p>
     * 关联到 {@code demand} 表，标识此附件属于哪个需求
     * </p>
     */
    private Long demandId;

    /**
     * 原始文件名
     * <p>
     * 用户上传时的文件名，例如："需求文档.pdf"、"原型图.png"
     * 用于前端展示和下载时的文件名
     * </p>
     */
    private String fileName;

    /**
     * 文件存储路径（相对路径）
     * <p>
     * 相对于上传根目录的路径，例如："2026/04/23/abc123.pdf"
     * 实际存储位置：{@code ${file.upload.path}/2026/04/23/abc123.pdf}
     * </p>
     * 
     * <p>
     * <b>路径格式</b>：{@code yyyy/MM/dd/uuid.ext}
     * </p>
     */
    private String filePath;

    /**
     * 文件扩展名
     * <p>
     * 小写格式，例如："pdf"、"png"、"docx"
     * 用于文件类型校验和前端图标显示
     * </p>
     */
    private String fileType;

    /**
     * 文件大小（字节）
     * <p>
     * 例如：1048576 = 1MB
     * </p>
     */
    private Long fileSize;

    /**
     * 上传者 ID
     * <p>
     * 关联到 {@code user} 表，记录谁上传了此附件
     * </p>
     */
    private Long uploaderId;

    /**
     * 文件 MD5 哈希值
     * <p>
     * 用于文件去重和完整性校验。
     * 相同内容的文件具有相同的 MD5 值，即使文件名不同。
     * </p>
     * 
     * <p>
     * <b>示例</b>：{@code d41d8cd98f00b204e9800998ecf8427e}
     * </p>
     */
    private String fileHash;

    /**
     * 上传时间
     * <p>
     * 精确到秒，用于排序和审计
     * </p>
     */
    private LocalDateTime createTime;
}
