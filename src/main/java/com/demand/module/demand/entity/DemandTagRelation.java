package com.demand.module.demand.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 需求标签关联实体类
 */
@Data
@TableName("demand_tag_relation")
public class DemandTagRelation {
    /**
     * 需求ID
     */
    private Long demandId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
