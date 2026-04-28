package com.demand.module.project.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.project.entity.Project;
import com.demand.module.project.entity.ProjectMember;
import com.demand.module.project.service.ProjectService;
import com.demand.module.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目控制器
 */
@Slf4j
@Tag(name = "项目管理", description = "项目和迭代管理接口")
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final PermissionService permissionService;

    @Operation(summary = "查询项目列表", description = "分页查询项目列表")
    @GetMapping("/list")
    public Result<List<Project>> getProjectList(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        Long currentUserId = permissionService.getCurrentUserId();
        List<Project> projects = projectService.getProjectList(keyword, status, currentUserId);
        return Result.success(projects);
    }

    @Operation(summary = "查询项目详情", description = "根据ID查询项目详细信息")
    @GetMapping("/{id}")
    public Result<Project> getProjectById(@Parameter(description = "项目ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        Project project = projectService.getProjectById(id, currentUserId);
        return Result.success(project);
    }

    @Operation(summary = "创建项目", description = "创建新项目")
    @PostMapping
    @RequirePermission(roles = {1, 4})
    public Result<?> createProject(@RequestBody Project project) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectService.createProject(project, currentUserId);
        return Result.success();
    }

    @Operation(summary = "更新项目", description = "更新项目信息")
    @PutMapping("/{id}")
    @RequirePermission(roles = {1, 4})
    public Result<?> updateProject(@Parameter(description = "项目ID") @PathVariable Long id,
                                   @RequestBody Project project) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectService.updateProject(id, project, currentUserId);
        return Result.success();
    }

    @Operation(summary = "删除项目", description = "删除项目")
    @DeleteMapping("/{id}")
    @RequirePermission(roles = {1, 4})
    public Result<?> deleteProject(@Parameter(description = "项目ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        projectService.deleteProject(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "查询项目成员", description = "查询项目的所有成员")
    @GetMapping("/{id}/members")
    public Result<List<ProjectMember>> getProjectMembers(@Parameter(description = "项目ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        List<ProjectMember> members = projectService.getProjectMembers(id, currentUserId);
        return Result.success(members);
    }

    @Operation(summary = "添加项目成员", description = "向项目添加成员（不指定角色时自动根据系统角色分配）")
    @PostMapping("/{id}/members")
    @RequirePermission(roles = {1, 4})
    public Result<?> addProjectMember(@Parameter(description = "项目ID") @PathVariable Long id,
                                       @RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String roleCode = params.containsKey("roleCode") ? params.get("roleCode").toString() : null;
        Long operatorId = permissionService.getCurrentUserId();
        projectService.addProjectMember(id, userId, roleCode, operatorId);
        return Result.success();
    }

    @Operation(summary = "更新成员角色", description = "更新项目成员的角色")
    @PutMapping("/{id}/members/{userId}/role")
    @RequirePermission(roles = {1, 4})
    public Result<?> updateMemberRole(@Parameter(description = "项目ID") @PathVariable Long id,
                                       @Parameter(description = "用户ID") @PathVariable Long userId,
                                       @RequestBody Map<String, String> params) {
        String newRoleCode = params.get("roleCode");
        Long operatorId = permissionService.getCurrentUserId();
        projectService.updateMemberRole(id, userId, newRoleCode, operatorId);
        return Result.success();
    }

    @Operation(summary = "移除项目成员", description = "从项目移除成员")
    @DeleteMapping("/{id}/members/{userId}")
    @RequirePermission(roles = {1, 4})
    public Result<?> removeProjectMember(@Parameter(description = "项目ID") @PathVariable Long id,
                                         @Parameter(description = "用户ID") @PathVariable Long userId) {
        Long operatorId = permissionService.getCurrentUserId();
        projectService.removeProjectMember(id, userId, operatorId);
        return Result.success();
    }
}
