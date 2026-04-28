package com.demand.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.project.entity.ProjectModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProjectModuleMapper extends BaseMapper<ProjectModule> {

    /**
     * 根据项目ID查询获取所有模块信息
     */
    List<ProjectModule> selectModulesByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据项目ID查询获取启用的模块信息
     */
    List<ProjectModule> selectActiveModulesByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据父级模块ID查询获取子级模块信息
     */
    List<ProjectModule> selectChildModules(@Param("parentId") Long parentId);
}
