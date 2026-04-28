package com.demand.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.project.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    /**
     * 根据项目ID查询项目成员信息
     */
    List<ProjectMember> selectMembersByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据项目ID和用户ID查询项目成员信息
     */
    ProjectMember selectMember(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
