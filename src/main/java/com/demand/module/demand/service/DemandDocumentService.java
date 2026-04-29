package com.demand.module.demand.service;

import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.DocumentCreateDTO;
import com.demand.module.demand.dto.DocumentReviewDTO;
import com.demand.module.demand.entity.DemandDocument;
import com.demand.module.demand.mapper.DemandDocumentMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求文档管理服务
 * <p>
 * 负责管理需求相关的各类文档，支持版本控制、评审流程和统计分析。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandDocumentService {

    private final DemandDocumentMapper documentMapper;
    private final UserMapper userMapper;

    /**
     * 上传/创建文档
     *
     * @param dto 文档信息
     * @param authorId 作者ID
     * @return 文档对象
     */
    @Transactional
    public DemandDocument createDocument(DocumentCreateDTO dto, Long authorId) {
        log.info("创建需求文档: demandId={}, docType={}, title={}",
                dto.getDemandId(), dto.getDocType(), dto.getTitle());

        // 1. 获取最新版本号
        Integer latestVersion = documentMapper.getMaxVersionNumber(dto.getDemandId(), dto.getDocType());
        Integer newVersion = latestVersion + 1;

        // 2. 获取作者信息
        User author = userMapper.findById(authorId);
        String authorName = author != null ? author.getRealName() : "未知";

        // 3. 标记旧版本为非最新
        if (latestVersion > 0) {
            documentMapper.markOldVersionsAsNotLatest(dto.getDemandId(), dto.getDocType());
        }

        // 4. 创建新文档
        DemandDocument document = new DemandDocument();
        document.setDemandId(dto.getDemandId());
        document.setDocType(dto.getDocType());
        document.setTitle(dto.getTitle());
        document.setDescription(dto.getDescription());
        document.setFileUrl(dto.getFileUrl());
        document.setFileName(dto.getFileName());
        document.setFileSize(dto.getFileSize());
        document.setFileType(dto.getFileType());
        document.setVersionNumber(newVersion);
        document.setVersionNote(dto.getVersionNote());
        document.setIsLatest(true);
        document.setAuthorId(authorId);
        document.setAuthorName(authorName);
        document.setReviewStatus("PENDING");
        document.setAccessLevel(dto.getAccessLevel() != null ? dto.getAccessLevel() : "PUBLIC");
        document.setDownloadCount(0);
        document.setViewCount(0);
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.insert(document);

        log.info("文档创建成功: documentId={}, version={}", document.getId(), newVersion);
        return document;
    }

    /**
     * 查询需求的所有文档
     *
     * @param demandId 需求 ID
     * @return 文档列表
     */
    public List<DemandDocument> getDocumentsByDemandId(Long demandId) {
        log.debug("查询需求文档: demandId={}", demandId);
        return documentMapper.selectByDemandId(demandId);
    }

    /**
     * 查询需求的最新文档（每种类型只返回最新版本）
     *
     * @param demandId 需求 ID
     * @return 最新文档列表
     */
    public List<DemandDocument> getLatestDocuments(Long demandId) {
        log.debug("查询需求最新文档: demandId={}", demandId);
        return documentMapper.selectLatestByDemandId(demandId);
    }

    /**
     * 查看文档（增加浏览次数）
     *
     * @param documentId 文档 ID
     * @return 文档对象
     */
    public DemandDocument viewDocument(Long documentId) {
        log.debug("查看文档: documentId={}", documentId);
        documentMapper.incrementViewCount(documentId);
        return documentMapper.selectById(documentId);
    }

    /**
     * 下载文档（增加下载次数）
     *
     * @param documentId 文档 ID
     * @return 文档对象
     */
    public DemandDocument downloadDocument(Long documentId) {
        log.debug("下载文档: documentId={}", documentId);
        documentMapper.incrementDownloadCount(documentId);
        return documentMapper.selectById(documentId);
    }

    /**
     * 评审文档
     *
     * @param documentId 文档 ID
     * @param reviewDTO 评审信息
     * @param reviewerId 评审人 ID
     */
    @Transactional
    public void reviewDocument(Long documentId, DocumentReviewDTO reviewDTO, Long reviewerId) {
        log.info("评审文档: documentId={}, status={}, reviewerId={}",
                documentId, reviewDTO.getReviewStatus(), reviewerId);

        DemandDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        // 获取评审人信息
        User reviewer = userMapper.findById(reviewerId);
        String reviewerName = reviewer != null ? reviewer.getRealName() : "系统";

        // 更新评审信息
        document.setReviewerId(reviewerId);
        document.setReviewerName(reviewerName);
        document.setReviewStatus(reviewDTO.getReviewStatus());
        document.setReviewComment(reviewDTO.getReviewComment());
        document.setReviewTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.updateById(document);

        log.info("文档评审完成: documentId={}, status={}", documentId, reviewDTO.getReviewStatus());
    }

    /**
     * 删除文档（软删除）
     *
     * @param documentId 文档 ID
     * @param operatorId 操作人 ID
     */
    @Transactional
    public void deleteDocument(Long documentId, Long operatorId) {
        log.info("删除文档: documentId={}, operatorId={}", documentId, operatorId);

        DemandDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        document.setDeleteTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);

        log.info("文档删除成功: documentId={}", documentId);
    }

    /**
     * 获取文档类型标签
     *
     * @param docType 文档类型代码
     * @return 中文标签
     */
    public String getDocTypeLabel(String docType) {
        return switch (docType) {
            case "PRD" -> "产品需求文档";
            case "UI_DESIGN" -> "UI设计稿";
            case "API_DOC" -> "API文档";
            case "TECH_DESIGN" -> "技术设计";
            case "TEST_CASE" -> "测试用例";
            case "USER_MANUAL" -> "用户手册";
            case "OTHER" -> "其他";
            default -> "未知";
        };
    }
}
