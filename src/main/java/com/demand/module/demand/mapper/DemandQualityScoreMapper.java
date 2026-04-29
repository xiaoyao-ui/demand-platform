package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.demand.entity.DemandQualityScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 需求质量评分数据访问层
 */
@Mapper
public interface DemandQualityScoreMapper extends BaseMapper<DemandQualityScore> {

    /**
     * 查询低质量需求（分数低于指定阈值）
     *
     * @param threshold 分数阈值
     * @return 低质量需求列表
     */
    @Select("SELECT d.*, s.total_score, s.rating " +
            "FROM demand d " +
            "INNER JOIN demand_quality_score s ON d.id = s.demand_id " +
            "WHERE s.total_score < #{threshold} " +
            "AND s.notified = 0 " +
            "AND d.delete_time IS NULL " +
            "ORDER BY s.total_score ASC")
    List<DemandQualityScore> findLowQualityDemands(@Param("threshold") Integer threshold);

    /**
     * 标记为已提醒
     *
     * @param demandId 需求 ID
     */
    @Update("UPDATE demand_quality_score SET notified = 1 WHERE demand_id = #{demandId}")
    void markAsNotified(@Param("demandId") Long demandId);
}
