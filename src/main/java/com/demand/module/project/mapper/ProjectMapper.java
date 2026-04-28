package com.demand.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查询项目列表
     */
    List<Project> selectProjectList(@Param("keyword") String keyword,
                                    @Param("status") Integer status);

    /**
     * 查询项目详情
     */
    Project selectProjectWithOwner(@Param("id") Long id);
}
