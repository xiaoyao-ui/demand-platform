package com.demand.module.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.demand.entity.DemandTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DemandTagMapper extends BaseMapper<DemandTag> {

    /**
     * 根据需求ID查询标签列表
     */
    List<DemandTag> selectTagsByDemandId(@Param("demandId") Long demandId);

    /**
     * 增加需求标签的使用次数
     */
    void incrementUseCount(@Param("tagId") Long tagId);

    /**
     * 减少需求标签的使用次数
     */
    void decrementUseCount(@Param("tagId") Long tagId);
}
