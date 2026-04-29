package com.demand.module.demand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 需求文档实体类
 * <p>
 * 管理需求相关的各类文档，支持版本控制和评审流程。
 * </p>
 */
@Data
@TableName("demand_document")
public class DemandDocument {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 需求 ID
     */
    private Long demandId;

    /**
     * 文档类型
     * <p>
     * PRD - 产品需求文档
     * UI_DESIGN - UI设计稿
     * API_DOC - API文档
     * TECH_DESIGN - 技术设计文档
     * TEST_CASE - 测试用例
     * USER_MANUAL - 用户手册
     * OTHER - 其他
     * </p>
     */
    private String docType;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档描述
     */
    private String description;

    /**
     * 文件 URL（在线文档链接或附件地址）
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（pdf, docx, xlsx, pptx, md, link 等）
     */
    private String fileType;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 版本说明
     */
    private String versionNote;

    /**
     * 是否最新版本
     */
    private Boolean isLatest;

    /**
     * 作者 ID
     */
    private Long authorId;

    /**
     * 作者姓名
     */
    private String authorName;

    /**
     * 评审人 ID
     */
    private Long reviewerId;

    /**
     * 评审人姓名
     */
    private String reviewerName;

    /**
     * 评审状态：PENDING-待评审, APPROVED-已通过, REJECTED-需修改
     */
    private String reviewStatus;

    /**
     * 评审意见
     */
    private String reviewComment;

    /**
     * 评审时间
     */
    private LocalDateTime reviewTime;

    /**
     * 访问级别：PUBLIC-公开, INTERNAL-内部, CONFIDENTIAL-机密
     */
    private String accessLevel;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 查看次数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;
}
