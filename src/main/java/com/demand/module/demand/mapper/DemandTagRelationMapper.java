package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.demand.entity.DemandTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DemandTagRelationMapper extends BaseMapper<DemandTagRelation> {

    /**
     * 批量新增需求标签关联关系
     */
    void batchInsert(@Param("relations") List<DemandTagRelation> relations);

    /**
     * 根据需求ID删除需求标签关联关系
     */
    void deleteByDemandId(@Param("demandId") Long demandId);
}
