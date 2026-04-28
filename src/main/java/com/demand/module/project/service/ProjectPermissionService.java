package com.demand.module.project.service;

import com.demand.exception.BusinessException;
import com.demand.module.project.entity.Project;
import com.demand.module.project.entity.ProjectMember;
import com.demand.module.project.mapper.ProjectMapper;
import com.demand.module.project.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPermissionService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    public ProjectMember getProjectMember(Long projectId, Long userId) {
        return projectMemberMapper.selectMember(projectId, userId);
    }

    public boolean isProjectOwner(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.isOwner();
    }

    public boolean isProjectManager(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.isManager();
    }

    public boolean canManageProject(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canManageProject();
    }

    public boolean canManageIteration(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canManageIteration();
    }

    public boolean canViewProject(Long projectId, Long userId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        if (project.getVisibility() == 1) {
            return true;
        }

        ProjectMember member = getProjectMember(projectId, userId);
        return member != null;
    }

    public boolean canViewDemandInProject(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canViewDemand();
    }

    public boolean canCreateDemandInProject(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canCreateDemand();
    }

    public boolean canEditDemandInProject(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canEditDemand();
    }

    public boolean canApproveDemandInProject(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canApproveDemand();
    }

    public boolean canAssignDemandInProject(Long projectId, Long userId) {
        ProjectMember member = getProjectMember(projectId, userId);
        return member != null && member.canAssignDemand();
    }

    public void checkProjectOwner(Long projectId, Long userId) {
        if (!isProjectOwner(projectId, userId)) {
            throw new BusinessException("只有项目负责人可以执行此操作");
        }
    }

    public void checkProjectManager(Long projectId, Long userId) {
        if (!isProjectManager(projectId, userId)) {
            throw new BusinessException("只有项目经理可以执行此操作");
        }
    }

    public void checkCanManageProject(Long projectId, Long userId) {
        if (!canManageProject(projectId, userId)) {
            throw new BusinessException("没有项目管理权限");
        }
    }

    public void checkCanManageIteration(Long projectId, Long userId) {
        if (!canManageIteration(projectId, userId)) {
            throw new BusinessException("没有迭代管理权限");
        }
    }

    public void checkCanViewProject(Long projectId, Long userId) {
        if (!canViewProject(projectId, userId)) {
            throw new BusinessException("没有项目查看权限");
        }
    }

    public void checkCanCreateDemandInProject(Long projectId, Long userId) {
        if (!canCreateDemandInProject(projectId, userId)) {
            throw new BusinessException("没有在此项目中创建需求的权限");
        }
    }

    public List<ProjectMember> getProjectMembersWithPermission(Long projectId, Long userId) {
        checkCanViewProject(projectId, userId);
        return projectMemberMapper.selectMembersByProjectId(projectId);
    }
}
