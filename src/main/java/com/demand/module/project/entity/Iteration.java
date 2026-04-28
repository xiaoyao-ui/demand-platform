package com.demand.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目迭代实体类
 */
@Data
@TableName("sys_iteration")
public class Iteration {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 迭代名称
     */
    private String name;

    /**
     * 迭代开始时间
     */
    private LocalDateTime startTime;

    /**
     * 迭代结束时间
     */
    private LocalDateTime endTime;

    /**
     * 迭代状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 项目名称
     */
    private String projectName;
}
