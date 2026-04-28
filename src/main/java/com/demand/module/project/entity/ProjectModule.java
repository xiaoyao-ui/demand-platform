package com.demand.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目模块实体类
 */
@Data
@TableName("project_module")
public class ProjectModule {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 模块名称
     */
    private String name;

    /**
     * 模块编码
     */
    private String code;

    /**
     * 模块描述
     */
    private String description;

    /**
     * 父级模块ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 子级模块
     */
    private List<ProjectModule> children;
}
