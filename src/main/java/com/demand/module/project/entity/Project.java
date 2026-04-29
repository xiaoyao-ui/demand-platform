package com.demand.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目实体类
 */
@Data
@TableName("sys_project")
public class Project {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目编码
     */
    private String code;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 项目负责人
     */
    private Long ownerId;

    /**
     * 项目可见性
     */
    private Integer visibility;

    /**
     * 项目状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 项目负责人名称
     * <p>
     * 从 {@code sys_user.real_name} 关联查询得到，用于前端展示
     * </p>
     */
    private String ownerName;

    /**
     * 项目负责人名称（别名，与 ownerName 相同）
     * <p>
     * 为了保持命名一致性，提供 managerName 作为 ownerName 的别名
     * </p>
     */
    public String getManagerName() {
        return this.ownerName;
    }
}
