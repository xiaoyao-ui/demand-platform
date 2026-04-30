package com.demand.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置组查询配置列表
     */
    List<SysConfig> selectByGroup(@Param("configGroup") String configGroup);

    /**
     * 根据配置键查询配置值
     */
    String selectValueByKey(@Param("configKey") String configKey);
}
