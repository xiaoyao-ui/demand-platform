package com.demand.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

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

    /**
     * 查询所有项目
     */
    List<Project> selectAll();

    /**
     * 获取项目周期内的需求列表
     */
    @Select("SELECT d.*, u.real_name AS assigneeName " +
            "FROM demand d " +
            "LEFT JOIN sys_user u ON d.assignee_id = u.id " +
            "WHERE d.project_id = #{projectId} " +
            "AND d.delete_time IS NULL " +
            "AND d.create_time >= #{startDate} " +
            "AND d.create_time <= #{endDate} " +
            "ORDER BY d.create_time DESC")
    List<Map<String, Object>> getDemandsInPeriod(@Param("projectId") Long projectId,
                                                 @Param("startDate") java.time.LocalDateTime startDate,
                                                 @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 获取项目在周期内完成的需求
     */
    @Select("SELECT d.*, u.real_name AS assigneeName " +
            "FROM demand d " +
            "LEFT JOIN sys_user u ON d.assignee_id = u.id " +
            "WHERE d.project_id = #{projectId} " +
            "AND d.delete_time IS NULL " +
            "AND d.status = 'COMPLETED' " +
            "AND d.complete_time >= #{startDate} " +
            "AND d.complete_time <= #{endDate} " +
            "ORDER BY d.complete_time DESC")
    List<Map<String, Object>> getCompletedDemandsInPeriod(@Param("projectId") Long projectId,
                                                          @Param("startDate") java.time.LocalDateTime startDate,
                                                          @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 获取项目在周期内的成员工作量统计
     */
    @Select("SELECT " +
            "d.assignee_id AS userId, " +
            "u.real_name AS userName, " +
            "COUNT(*) AS demandCount, " +
            "SUM(CASE WHEN d.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount, " +
            "COALESCE(SUM(d.estimated_hours), 0) AS estimatedHours, " +
            "COALESCE(SUM(d.actual_hours), 0) AS actualHours " +
            "FROM demand d " +
            "LEFT JOIN sys_user u ON d.assignee_id = u.id " +
            "WHERE d.project_id = #{projectId} " +
            "AND d.assignee_id IS NOT NULL " +
            "AND d.delete_time IS NULL " +
            "AND d.create_time >= #{startDate} " +
            "AND d.create_time <= #{endDate} " +
            "GROUP BY d.assignee_id, u.real_name " +
            "ORDER BY actualHours DESC")
    List<Map<String, Object>> getMemberWorkloadInPeriod(@Param("projectId") Long projectId,
                                                        @Param("startDate") java.time.LocalDateTime startDate,
                                                        @Param("endDate") java.time.LocalDateTime endDate);
}
