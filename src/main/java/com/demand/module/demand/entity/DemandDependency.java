package com.demand.module.demand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 需求依赖关系实体类
 */
@Data
@TableName("demand_dependency")
public class DemandDependency {
    /**
     * 依赖ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 当前需求ID
     */
    private Long demandId;

    /**
     * 依赖的需求ID
     */
    private Long dependsOnId;

    /**
     * 依赖类型
     */
    private String dependencyType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 依赖的需求标题
     */
    private String dependsOnTitle;

    /**
     * 依赖的需求状态
     */
    private String dependsOnStatus;
}
