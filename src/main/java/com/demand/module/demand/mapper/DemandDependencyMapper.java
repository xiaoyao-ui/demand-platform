package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.demand.entity.DemandDependency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DemandDependencyMapper extends BaseMapper<DemandDependency> {

    /**
     * 查询指定需求下的所有依赖关系
     */
    List<DemandDependency> selectDependenciesByDemandId(@Param("demandId") Long demandId);

    /**
     * 查询指定需求所依赖的其它需求
     */
    List<DemandDependency> selectDependentDemands(@Param("demandId") Long demandId);
}
