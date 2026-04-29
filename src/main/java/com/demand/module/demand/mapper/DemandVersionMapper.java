package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.demand.entity.DemandVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 需求版本数据访问层
 */
@Mapper
public interface DemandVersionMapper extends BaseMapper<DemandVersion> {

    /**
     * 查询需求的所有版本（按版本号降序）
     *
     * @param demandId 需求 ID
     * @return 版本列表
     */
    @Select("SELECT * FROM demand_version WHERE demand_id = #{demandId} ORDER BY version_number DESC")
    List<DemandVersion> selectByDemandId(@Param("demandId") Long demandId);

    /**
     * 获取需求的最新版本号
     *
     * @param demandId 需求 ID
     * @return 最新版本号，如果没有版本则返回 0
     */
    @Select("SELECT COALESCE(MAX(version_number), 0) FROM demand_version WHERE demand_id = #{demandId}")
    Integer getMaxVersionNumber(@Param("demandId") Long demandId);

    /**
     * 查询指定版本
     *
     * @param demandId 需求 ID
     * @param versionNumber 版本号
     * @return 版本对象
     */
    @Select("SELECT * FROM demand_version WHERE demand_id = #{demandId} AND version_number = #{versionNumber}")
    DemandVersion selectByVersion(@Param("demandId") Long demandId, @Param("versionNumber") Integer versionNumber);
}
