package com.demand.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.project.entity.Iteration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface IterationMapper extends BaseMapper<Iteration> {

    /**
     * 查询项目下的所有迭代
     */
    List<Iteration> selectIterationsByProjectId(@Param("projectId") Long projectId);

    /**
     * 查询项目下的活跃迭代
     */
    List<Iteration> selectActiveIterations(@Param("projectId") Long projectId);
}
