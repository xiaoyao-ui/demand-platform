package com.demand.module.demand.mapper;

import com.demand.module.demand.entity.DemandActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 需求动态时间轴数据访问层
 * <p>
 * 提供需求操作动态的插入和查询操作。
 * 动态记录包括：创建、审批、分配、状态变更等关键操作。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>保存动态</b>：记录需求的关键操作到数据库</li>
 *   <li><b>查询动态</b>：返回指定需求的所有操作记录，按时间倒序排列</li>
 * </ul>
 * 
 * <h3>典型动态类型：</h3>
 * <ul>
 *   <li>CREATE - 创建需求</li>
 *   <li>SUBMIT - 提交审核</li>
 *   <li>APPROVE - 审批需求</li>
 *   <li>ASSIGN - 分配负责人</li>
 *   <li>STATUS_CHANGE - 状态变更</li>
 *   <li>ATTACHMENT - 上传附件</li>
 *   <li>WITHDRAW - 撤回需求</li>
 * </ul>
 */
@Mapper
public interface DemandActivityMapper {

    /**
     * 插入需求动态记录
     * <p>
     * 在需求的关键操作后调用此方法，记录操作信息。
     * 自动填充创建时间为当前时间。
     * </p>
     * 
     * <p>
     * <b>调用时机</b>：
     * <ul>
     *   <li>创建需求后</li>
     *   <li>提交审核后</li>
     *   <li>审批通过后</li>
     *   <li>分配负责人后</li>
     *   <li>状态变更后</li>
     *   <li>上传/删除附件后</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>SQL 实现</b>：参见 {@code DemandActivityMapper.xml} 中的 {@code insert} 标签
     * </p>
     *
     * @param activity 动态对象
     */
    void insert(DemandActivity activity);

    /**
     * 查询需求的所有动态记录
     * <p>
     * 返回指定需求的所有操作记录，按创建时间倒序排列（最新的在前）。
     * 用于前端展示需求的生命周期时间轴。
     * </p>
     * 
     * <p>
     * <b>SQL 实现</b>：参见 {@code DemandActivityMapper.xml} 中的 {@code selectByDemandId} 标签
     * </p>
     *
     * @param demandId 需求 ID
     * @return 动态列表（按 create_time DESC 排序）
     */
    List<DemandActivity> selectByDemandId(@Param("demandId") Long demandId);
}
