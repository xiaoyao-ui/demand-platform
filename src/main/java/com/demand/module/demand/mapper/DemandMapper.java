package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.common.PageResult;
import com.demand.module.demand.dto.DemandQueryDTO;
import com.demand.module.demand.entity.Demand;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 需求数据访问层
 */
@Mapper
public interface DemandMapper extends BaseMapper<Demand> {

    /**
     * 新增需求
     */
    int insert(Demand demand);

    /**
     * 根据需求ID查询需求信息
     */
    Demand findById(Long id);

    /**
     * 更新需求
     */
    int update(Demand demand);

    /**
     * 物理删除需求
     */
    int deleteById(Long id);

    /**
     * 根据条件查询需求信息
     */
    List<Demand> findByCondition(@Param("keyword") String keyword,
                                 @Param("type") String type,
                                 @Param("priority") String priority,
                                 @Param("status") String status,
                                 @Param("proposerId") Long proposerId,
                                 @Param("assigneeId") Long assigneeId,
                                 @Param("offset") Integer offset,
                                 @Param("pageSize") Integer pageSize);

    /**
     * 查询所有未删除的需求信息
     */
    List<Demand> selectAll();

    /**
     * 统计需求条数
     * @return
     */
    Long countByCondition(@Param("keyword") String keyword,
                          @Param("type") String type,
                          @Param("priority") String priority,
                          @Param("status") String status,
                          @Param("proposerId") Long proposerId,
                          @Param("assigneeId") Long assigneeId);

    /**
     * 统计各状态的需求数量
     */
    List<Map<String, Object>> countByStatusGroup();

    /**
     * 统计各类型的需求数量
     */
    List<Map<String, Object>> countByTypeGroup();

    /**
     * 统计各优先级的需求数量
     */
    List<Map<String, Object>> countByPriorityGroup();

    /**
     * 查询近7天每天的需求创建数量
     */
    List<Map<String, Object>> countByDateForLast7Days();

    /**
     * 查询待审批超过指定小时数的需求
     */
    List<Demand> findPendingApprovalOverHours(@Param("hours") Integer hours);

    /**
     * 分页查询需求列表
     */
    PageResult<Demand> selectDemandPage(@Param("query") DemandQueryDTO queryDTO);

    /**
     * 查询需求详情
     */
    Demand selectDemandWithDetails(@Param("id") Long id);

    /**
     * 查询指定项目下的需求列表
     */
    List<Demand> selectDemandsByProjectId(@Param("projectId") Long projectId);

    /**
     * 统计指定状态的需求条数
     */
    int countByStatus(@Param("status") String status);

    /**
     * 统计指定项目下的指定状态的需求条数
     */
    int countByProjectAndStatus(@Param("projectId") Long projectId, @Param("status") String status);

    /**
     * 获取项目统计数据
     */
    Map<String, Object> getProjectStats(@Param("projectId") Long projectId);

    /**
     * 获取项目下需求的状态分布
     */
    List<Map<String, Object>> getProjectStatusDistribution(@Param("projectId") Long projectId);

    /**
     * 获取项目下需求的类型分布
     */
    List<Map<String, Object>> getProjectTypeDistribution(@Param("projectId") Long projectId);

    /**
     * 获取迭代看板数据
     */
    Map<String, Object> getIterationKanban(@Param("iterationId") Long iterationId);

    /**
     * 获取迭代内需求的状态分布
     */
    List<Map<String, Object>> getIterationStatusDistribution(@Param("iterationId") Long iterationId);

    /**
     * 获取迭代内需求的负责人分布
     */
    List<Map<String, Object>> getIterationAssigneeDistribution(@Param("iterationId") Long iterationId);

    /**
     * 获取迭代每日完成趋势（用于燃尽图）
     */
    List<Map<String, Object>> getIterationDailyProgress(@Param("iterationId") Long iterationId);
}
