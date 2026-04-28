package com.demand.module.project.service;

import com.demand.exception.BusinessException;
import com.demand.module.project.entity.Project;
import com.demand.module.project.entity.ProjectMember;
import com.demand.module.project.mapper.ProjectMapper;
import com.demand.module.project.mapper.ProjectMemberMapper;
import com.demand.module.project.util.ProjectRoleMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.RoleMapper;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目业务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ProjectPermissionService permissionService;

    public List<Project> getProjectList(String keyword, Integer status, Long currentUserId) {
        List<Project> allProjects = projectMapper.selectProjectList(keyword, status);
        
        return allProjects.stream()
                .filter(project -> {
                    if (project.getVisibility() == 1) {
                        return true;
                    }
                    return permissionService.canViewProject(project.getId(), currentUserId);
                })
                .toList();
    }

    public Project getProjectById(Long id, Long currentUserId) {
        permissionService.checkCanViewProject(id, currentUserId);
        
        Project project = projectMapper.selectProjectWithOwner(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }

    @Transactional
    public void createProject(Project project, Long currentUserId) {
        log.info("创建项目: name={}, ownerId={}", project.getName(), currentUserId);

        project.setOwnerId(currentUserId);
        project.setStatus(1);
        project.setCreateTime(LocalDateTime.now());
        projectMapper.insert(project);

        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getId());
        member.setUserId(currentUserId);
        member.setRoleCode("OWNER");
        member.setJoinTime(LocalDateTime.now());
        projectMemberMapper.insert(member);

        log.info("项目创建成功: projectId={}", project.getId());
    }

    @Transactional
    public void updateProject(Long id, Project project, Long currentUserId) {
        permissionService.checkCanManageProject(id, currentUserId);

        Project existProject = projectMapper.selectById(id);
        if (existProject == null) {
            throw new BusinessException("项目不存在");
        }

        project.setId(id);
        projectMapper.updateById(project);
        log.info("项目更新成功: projectId={}", id);
    }

    @Transactional
    public void deleteProject(Long id, Long currentUserId) {
        permissionService.checkProjectOwner(id, currentUserId);

        Project existProject = projectMapper.selectById(id);
        if (existProject == null) {
            throw new BusinessException("项目不存在");
        }

        projectMapper.deleteById(id);
        log.info("项目删除成功: projectId={}", id);
    }

    public List<ProjectMember> getProjectMembers(Long projectId, Long currentUserId) {
        return permissionService.getProjectMembersWithPermission(projectId, currentUserId);
    }

    @Transactional
    public void addProjectMember(Long projectId, Long userId, String roleCode, Long operatorId) {
        permissionService.checkCanManageProject(projectId, operatorId);

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        ProjectMember existMember = projectMemberMapper.selectMember(projectId, userId);
        if (existMember != null) {
            throw new BusinessException("该用户已是项目成员");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 如果未指定项目角色，根据系统角色自动分配默认角色
        if (roleCode == null || roleCode.isEmpty()) {
            List<String> systemRoles = roleMapper.selectRoleKeysByUserId(userId);
            roleCode = ProjectRoleMapper.recommendProjectRole(systemRoles);
            log.info("为用户自动分配项目角色: userId={}, systemRoles={}, projectRole={}", 
                    userId, systemRoles, roleCode);
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRoleCode(roleCode);
        member.setJoinTime(LocalDateTime.now());
        projectMemberMapper.insert(member);

        log.info("添加项目成员成功: projectId={}, userId={}, roleCode={}", projectId, userId, roleCode);
    }

    @Transactional
    public void removeProjectMember(Long projectId, Long userId, Long operatorId) {
        permissionService.checkCanManageProject(projectId, operatorId);

        ProjectMember member = projectMemberMapper.selectMember(projectId, userId);
        if (member == null) {
            throw new BusinessException("该用户不是项目成员");
        }

        if (member.isOwner()) {
            throw new BusinessException("不能移除项目负责人");
        }

        projectMemberMapper.deleteById(member.getId());
        log.info("移除项目成员成功: projectId={}, userId={}", projectId, userId);
    }

    @Transactional
    public void updateMemberRole(Long projectId, Long userId, String newRoleCode, Long operatorId) {
        permissionService.checkProjectOwner(projectId, operatorId);

        ProjectMember member = projectMemberMapper.selectMember(projectId, userId);
        if (member == null) {
            throw new BusinessException("该用户不是项目成员");
        }

        if (member.isOwner()) {
            throw new BusinessException("不能修改项目负责人的角色");
        }

        member.setRoleCode(newRoleCode);
        projectMemberMapper.updateById(member);
        log.info("更新成员角色成功: projectId={}, userId={}, newRoleCode={}", projectId, userId, newRoleCode);
    }
}
