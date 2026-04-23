package com.demand.module.demand.mapper;

import com.demand.module.demand.entity.Demand;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 需求数据访问层
 */
@Mapper
public interface DemandMapper {

    //新增数据
    @Insert("INSERT INTO demand(title, description, type, priority, status, proposer_id, assignee_id, module, " +
            "expected_date, create_time, update_time) " +
            "VALUES(#{title}, #{description}, #{type}, #{priority}, #{status}, #{proposerId}, #{assigneeId}, " +
            "#{module}, #{expectedDate}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Demand demand);

    //根据主键查询需求（关联查询用户名）
    @Select("SELECT d.*, " +
            "p.real_name AS proposerName, " +
            "a.real_name AS assigneeName, " +
            "ap.real_name AS approverName " +
            "FROM demand d " +
            "LEFT JOIN `user` p ON d.proposer_id = p.id " +
            "LEFT JOIN `user` a ON d.assignee_id = a.id " +
            "LEFT JOIN `user` ap ON d.approver_id = ap.id " +
            "WHERE d.id = #{id}")
    Demand findById(Long id);

    //更新需求
    @Update("<script>" +
            "UPDATE demand " +
            "<set>" +
            "<if test='title != null'>title=#{title},</if>" +
            "<if test='description != null'>description=#{description},</if>" +
            "<if test='type != null'>type=#{type},</if>" +
            "<if test='priority != null'>priority=#{priority},</if>" +
            "<if test='status != null'>status=#{status},</if>" +
            "<if test='assigneeId != null'>assignee_id=#{assigneeId},</if>" +
            "<if test='approverId != null'>approver_id=#{approverId},</if>" +
            "<if test='approveTime != null'>approve_time=#{approveTime},</if>" +
            "<if test='approveComment != null'>approve_comment=#{approveComment},</if>" +
            "<if test='module != null'>module=#{module},</if>" +
            "<if test='expectedDate != null'>expected_date=#{expectedDate},</if>" +
            "<if test='actualDate != null'>actual_date=#{actualDate},</if>" +
            "update_time=#{updateTime}" +
            "</set>" +
            "WHERE id=#{id}" +
            "</script>")
    int update(Demand demand);

    //物理删除需求
    @Delete("DELETE FROM demand WHERE id = #{id}")
    int deleteById(Long id);

    //需求查询（关联查询用户名）
    @Select("<script>" +
            "SELECT d.*, " +
            "p.real_name AS proposerName, " +
            "a.real_name AS assigneeName, " +
            "ap.real_name AS approverName " +
            "FROM demand d " +
            "LEFT JOIN `user` p ON d.proposer_id = p.id " +
            "LEFT JOIN `user` a ON d.assignee_id = a.id " +
            "LEFT JOIN `user` ap ON d.approver_id = ap.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (d.title LIKE CONCAT('%', #{keyword}, '%') OR d.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='type != null'>AND d.type = #{type}</if>" +
            "<if test='priority != null'>AND d.priority = #{priority}</if>" +
            "<if test='status != null'>AND d.status = #{status}</if>" +
            "<if test='proposerId != null'>AND d.proposer_id = #{proposerId}</if>" +
            "<if test='assigneeId != null'>AND d.assignee_id = #{assigneeId}</if>" +
            "ORDER BY d.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Demand> findByCondition(@Param("keyword") String keyword,
                                 @Param("type") Integer type,
                                 @Param("priority") Integer priority,
                                 @Param("status") Integer status,
                                 @Param("proposerId") Long proposerId,
                                 @Param("assigneeId") Long assigneeId,
                                 @Param("offset") Integer offset,
                                 @Param("pageSize") Integer pageSize);

    @Select("SELECT * FROM demand WHERE 1 = 1 ORDER BY create_time DESC")
    List<Demand> selectAll();

    //统计需求条数
    @Select("<script>" +
            "SELECT COUNT(*) FROM demand WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='type != null'>AND type = #{type}</if>" +
            "<if test='priority != null'>AND priority = #{priority}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='proposerId != null'>AND proposer_id = #{proposerId}</if>" +
            "<if test='assigneeId != null'>AND assignee_id = #{assigneeId}</if>" +
            "</script>")
    Long countByCondition(@Param("keyword") String keyword,
                          @Param("type") Integer type,
                          @Param("priority") Integer priority,
                          @Param("status") Integer status,
                          @Param("proposerId") Long proposerId,
                          @Param("assigneeId") Long assigneeId);

    // 统计各状态的需求数量
    @Select("SELECT status, COUNT(*) as count FROM demand GROUP BY status")
    @Results({
        @Result(property = "status", column = "status"),
        @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> countByStatus();

    // 统计各类型的需求数量
    @Select("SELECT type, COUNT(*) as count FROM demand GROUP BY type")
    @Results({
        @Result(property = "type", column = "type"),
        @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> countByType();

    // 统计各优先级的需求数量
    @Select("SELECT priority, COUNT(*) as count FROM demand GROUP BY priority")
    @Results({
        @Result(property = "priority", column = "priority"),
        @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> countByPriority();

    // 查询近7天每天的需求创建数量
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count " +
            "FROM demand " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date ASC")
    @Results({
        @Result(property = "date", column = "date"),
        @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> countByDateForLast7Days();

    /**
     * 查询待审批超过指定小时数的需求
     *
     * @param hours 小时数（例如 24 表示超过 24 小时）
     * @return 待审批需求列表（包含提出人信息）
     */
    @Select("SELECT d.*, p.real_name AS proposerName " +
            "FROM demand d " +
            "LEFT JOIN `user` p ON d.proposer_id = p.id " +
            "WHERE d.status = 0 " +
            "AND d.create_time < DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "ORDER BY d.create_time ASC")
    List<Demand> findPendingApprovalOverHours(@Param("hours") Integer hours);
}
