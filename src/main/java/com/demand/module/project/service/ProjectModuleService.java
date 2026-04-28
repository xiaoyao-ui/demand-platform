package com.demand.module.project.service;

import com.demand.exception.BusinessException;
import com.demand.module.project.entity.ProjectModule;
import com.demand.module.project.mapper.ProjectModuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目模块业务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectModuleService {

    private final ProjectModuleMapper projectModuleMapper;
    private final ProjectPermissionService permissionService;

    public List<ProjectModule> getModulesByProjectId(Long projectId, Long currentUserId) {
        permissionService.checkCanViewProject(projectId, currentUserId);

        List<ProjectModule> allModules = projectModuleMapper.selectModulesByProjectId(projectId);
        return buildModuleTree(allModules);
    }

    public List<ProjectModule> getActiveModulesByProjectId(Long projectId, Long currentUserId) {
        permissionService.checkCanViewProject(projectId, currentUserId);

        List<ProjectModule> allModules = projectModuleMapper.selectActiveModulesByProjectId(projectId);
        return buildModuleTree(allModules);
    }

    public ProjectModule getModuleById(Long id, Long currentUserId) {
        ProjectModule module = projectModuleMapper.selectById(id);
        if (module == null) {
            throw new BusinessException("模块不存在");
        }

        permissionService.checkCanViewProject(module.getProjectId(), currentUserId);
        return module;
    }

    @Transactional
    public void createModule(ProjectModule module, Long currentUserId) {
        permissionService.checkCanManageProject(module.getProjectId(), currentUserId);

        if (module.getCode() == null || module.getCode().isEmpty()) {
            throw new BusinessException("模块编码不能为空");
        }

        ProjectModule existModule = projectModuleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectModule>()
                        .eq(ProjectModule::getProjectId, module.getProjectId())
                        .eq(ProjectModule::getCode, module.getCode())
        ).stream().findFirst().orElse(null);

        if (existModule != null) {
            throw new BusinessException("该项目中已存在相同编码的模块");
        }

        if (module.getParentId() == null) {
            module.setParentId(0L);
        }

        if (module.getSort() == null) {
            module.setSort(0);
        }

        module.setStatus(1);
        module.setCreateTime(LocalDateTime.now());
        module.setUpdateTime(LocalDateTime.now());
        projectModuleMapper.insert(module);

        log.info("创建项目模块成功: moduleId={}, projectId={}, name={}",
                module.getId(), module.getProjectId(), module.getName());
    }

    @Transactional
    public void updateModule(Long id, ProjectModule module, Long currentUserId) {
        ProjectModule existModule = projectModuleMapper.selectById(id);
        if (existModule == null) {
            throw new BusinessException("模块不存在");
        }

        permissionService.checkCanManageProject(existModule.getProjectId(), currentUserId);

        module.setId(id);
        module.setUpdateTime(LocalDateTime.now());
        projectModuleMapper.updateById(module);

        log.info("更新项目模块成功: moduleId={}", id);
    }

    @Transactional
    public void deleteModule(Long id, Long currentUserId) {
        ProjectModule existModule = projectModuleMapper.selectById(id);
        if (existModule == null) {
            throw new BusinessException("模块不存在");
        }

        permissionService.checkCanManageProject(existModule.getProjectId(), currentUserId);

        List<ProjectModule> childModules = projectModuleMapper.selectChildModules(id);
        if (!childModules.isEmpty()) {
            throw new BusinessException("该模块下还有子模块，请先删除子模块");
        }

        projectModuleMapper.deleteById(id);
        log.info("删除项目模块成功: moduleId={}", id);
    }

    @Transactional
    public void disableModule(Long id, Long currentUserId) {
        ProjectModule existModule = projectModuleMapper.selectById(id);
        if (existModule == null) {
            throw new BusinessException("模块不存在");
        }

        permissionService.checkCanManageProject(existModule.getProjectId(), currentUserId);

        existModule.setStatus(0);
        existModule.setUpdateTime(LocalDateTime.now());
        projectModuleMapper.updateById(existModule);

        log.info("禁用项目模块成功: moduleId={}", id);
    }

    @Transactional
    public void enableModule(Long id, Long currentUserId) {
        ProjectModule existModule = projectModuleMapper.selectById(id);
        if (existModule == null) {
            throw new BusinessException("模块不存在");
        }

        permissionService.checkCanManageProject(existModule.getProjectId(), currentUserId);

        existModule.setStatus(1);
        existModule.setUpdateTime(LocalDateTime.now());
        projectModuleMapper.updateById(existModule);

        log.info("启用项目模块成功: moduleId={}", id);
    }

    private List<ProjectModule> buildModuleTree(List<ProjectModule> allModules) {
        if (allModules == null || allModules.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProjectModule> rootModules = allModules.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0)
                .collect(Collectors.toList());

        for (ProjectModule root : rootModules) {
            root.setChildren(buildChildren(root.getId(), allModules));
        }

        return rootModules;
    }

    private List<ProjectModule> buildChildren(Long parentId, List<ProjectModule> allModules) {
        List<ProjectModule> children = allModules.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .collect(Collectors.toList());

        for (ProjectModule child : children) {
            child.setChildren(buildChildren(child.getId(), allModules));
        }

        return children;
    }
}
