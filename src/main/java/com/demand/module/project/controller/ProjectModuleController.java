package com.demand.module.project.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.project.entity.ProjectModule;
import com.demand.module.project.service.ProjectModuleService;
import com.demand.module.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "项目模块管理", description = "项目模块的增删改查接口")
@RestController
@RequestMapping("/api/project/module")
@RequiredArgsConstructor
public class ProjectModuleController {

    private final ProjectModuleService projectModuleService;
    private final PermissionService permissionService;

    @Operation(summary = "查询项目模块列表", description = "查询指定项目的所有模块（树形结构）")
    @GetMapping("/project/{projectId}")
    public Result<List<ProjectModule>> getModulesByProjectId(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        Long currentUserId = permissionService.getCurrentUserId();
        List<ProjectModule> modules = projectModuleService.getModulesByProjectId(projectId, currentUserId);
        return Result.success(modules);
    }

    @Operation(summary = "查询启用的模块列表", description = "查询指定项目中启用的模块（树形结构）")
    @GetMapping("/project/{projectId}/active")
    public Result<List<ProjectModule>> getActiveModulesByProjectId(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        Long currentUserId = permissionService.getCurrentUserId();
        List<ProjectModule> modules = projectModuleService.getActiveModulesByProjectId(projectId, currentUserId);
        return Result.success(modules);
    }

    @Operation(summary = "查询模块详情", description = "根据ID查询模块详细信息")
    @GetMapping("/{id}")
    public Result<ProjectModule> getModuleById(@Parameter(description = "模块ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        ProjectModule module = projectModuleService.getModuleById(id, currentUserId);
        return Result.success(module);
    }

    @Operation(summary = "创建模块", description = "为项目创建新模块")
    @PostMapping
    @RequirePermission(roles = {1, 4})
    public Result<?> createModule(@RequestBody ProjectModule module) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectModuleService.createModule(module, currentUserId);
        return Result.success();
    }

    @Operation(summary = "更新模块", description = "更新模块信息")
    @PutMapping("/{id}")
    @RequirePermission(roles = {1, 4})
    public Result<?> updateModule(@Parameter(description = "模块ID") @PathVariable Long id,
                                  @RequestBody ProjectModule module) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectModuleService.updateModule(id, module, currentUserId);
        return Result.success();
    }

    @Operation(summary = "删除模块", description = "删除模块（不能有子模块）")
    @DeleteMapping("/{id}")
    @RequirePermission(roles = {1, 4})
    public Result<?> deleteModule(@Parameter(description = "模块ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectModuleService.deleteModule(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "禁用模块", description = "禁用模块（不删除数据）")
    @PostMapping("/{id}/disable")
    @RequirePermission(roles = {1, 4})
    public Result<?> disableModule(@Parameter(description = "模块ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectModuleService.disableModule(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "启用模块", description = "启用模块")
    @PostMapping("/{id}/enable")
    @RequirePermission(roles = {1, 4})
    public Result<?> enableModule(@Parameter(description = "模块ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectModuleService.enableModule(id, currentUserId);
        return Result.success();
    }
}
