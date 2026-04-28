package com.demand.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demand.common.ProjectRoleEnum;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目成员实体类
 */
@Data
@TableName("sys_project_member")
public class ProjectMember {
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
     * 成员ID
     */
    private Long userId;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 成员名称
     */
    private String userName;

    /**
     * 真实姓名
     */
    private String realName;

    public boolean isOwner() {
        return ProjectRoleEnum.OWNER.getCode().equals(this.roleCode);
    }

    public boolean isManager() {
        return ProjectRoleEnum.MANAGER.getCode().equals(this.roleCode) || isOwner();
    }

    public boolean canManageProject() {
        return isOwner() || isManager();
    }

    public boolean canManageIteration() {
        return isOwner() || isManager() ||
                ProjectRoleEnum.PRODUCT_MANAGER.getCode().equals(this.roleCode);
    }

    public boolean canViewDemand() {
        return true;
    }

    public boolean canCreateDemand() {
        return !ProjectRoleEnum.VIEWER.getCode().equals(this.roleCode);
    }

    public boolean canEditDemand() {
        return isOwner() || isManager() ||
                ProjectRoleEnum.PRODUCT_MANAGER.getCode().equals(this.roleCode) ||
                ProjectRoleEnum.DEVELOPER.getCode().equals(this.roleCode);
    }

    public boolean canApproveDemand() {
        return isOwner() || isManager();
    }

    public boolean canAssignDemand() {
        return isOwner() || isManager();
    }
}
