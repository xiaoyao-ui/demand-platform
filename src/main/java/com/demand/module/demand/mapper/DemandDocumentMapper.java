package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.demand.entity.DemandDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 需求文档数据访问层
 */
@Mapper
public interface DemandDocumentMapper extends BaseMapper<DemandDocument> {

    /**
     * 查询需求的所有文档（按类型分组，最新版本优先）
     *
     * @param demandId 需求 ID
     * @return 文档列表
     */
    @Select("SELECT * FROM demand_document " +
            "WHERE demand_id = #{demandId} " +
            "AND delete_time IS NULL " +
            "ORDER BY doc_type ASC, version_number DESC")
    List<DemandDocument> selectByDemandId(@Param("demandId") Long demandId);

    /**
     * 查询需求的最新文档列表（每种类型只返回最新版本）
     *
     * @param demandId 需求 ID
     * @return 最新文档列表
     */
    @Select("SELECT d1.* FROM demand_document d1 " +
            "INNER JOIN (" +
            "  SELECT doc_type, MAX(version_number) AS max_version " +
            "  FROM demand_document " +
            "  WHERE demand_id = #{demandId} AND delete_time IS NULL " +
            "  GROUP BY doc_type" +
            ") d2 ON d1.doc_type = d2.doc_type AND d1.version_number = d2.max_version " +
            "WHERE d1.demand_id = #{demandId} " +
            "ORDER BY d1.doc_type ASC")
    List<DemandDocument> selectLatestByDemandId(@Param("demandId") Long demandId);

    /**
     * 获取指定文档类型的最新版本号
     *
     * @param demandId 需求 ID
     * @param docType 文档类型
     * @return 最新版本号
     */
    @Select("SELECT COALESCE(MAX(version_number), 0) FROM demand_document " +
            "WHERE demand_id = #{demandId} AND doc_type = #{docType} AND delete_time IS NULL")
    Integer getMaxVersionNumber(@Param("demandId") Long demandId, @Param("docType") String docType);

    /**
     * 标记旧版本为非最新
     *
     * @param demandId 需求 ID
     * @param docType 文档类型
     */
    @Update("UPDATE demand_document SET is_latest = 0 " +
            "WHERE demand_id = #{demandId} AND doc_type = #{docType} AND delete_time IS NULL")
    void markOldVersionsAsNotLatest(@Param("demandId") Long demandId, @Param("docType") String docType);

    /**
     * 增加浏览次数
     *
     * @param documentId 文档 ID
     */
    @Update("UPDATE demand_document SET view_count = view_count + 1 WHERE id = #{documentId}")
    void incrementViewCount(@Param("documentId") Long documentId);

    /**
     * 增加下载次数
     *
     * @param documentId 文档 ID
     */
    @Update("UPDATE demand_document SET download_count = download_count + 1 WHERE id = #{documentId}")
    void incrementDownloadCount(@Param("documentId") Long documentId);
}
