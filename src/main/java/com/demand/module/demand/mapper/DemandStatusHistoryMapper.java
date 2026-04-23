package com.demand.module.demand.mapper;

import com.demand.module.demand.entity.DemandStatusHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 需求状态历史数据访问层
 */
@Mapper
public interface DemandStatusHistoryMapper {

    //新增需求状态历史
    @Insert("INSERT INTO demand_status_history(demand_id, old_status, new_status, remark, operator_id, create_time) " +
            "VALUES(#{demandId}, #{oldStatus}, #{newStatus}, #{remark}, #{operatorId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DemandStatusHistory history);

    //根据需求主键查询需求状态历史
    @Select("SELECT h.*, u.real_name AS operatorName " +
            "FROM demand_status_history h " +
            "LEFT JOIN user u ON h.operator_id = u.id " +
            "WHERE h.demand_id = #{demandId} " +
            "ORDER BY h.create_time DESC")
    List<DemandStatusHistory> findByDemandId(Long demandId);
}
