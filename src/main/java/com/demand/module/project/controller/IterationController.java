package com.demand.module.project.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.project.entity.Iteration;
import com.demand.module.project.service.IterationService;
import com.demand.module.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 迭代控制器
 */
@Slf4j
@Tag(name = "迭代管理", description = "迭代管理接口")
@RestController
@RequestMapping("/api/iteration")
@RequiredArgsConstructor
public class IterationController {

    private final IterationService iterationService;
    private final PermissionService permissionService;

    @Operation(summary = "查询项目迭代列表", description = "查询指定项目的所有迭代")
    @GetMapping("/project/{projectId}")
    public Result<List<Iteration>> getIterationsByProjectId(@Parameter(description = "项目ID") @PathVariable Long projectId) {
        Long currentUserId = permissionService.getCurrentUserId();
        List<Iteration> iterations = iterationService.getIterationsByProjectId(projectId, currentUserId);
        return Result.success(iterations);
    }

    @Operation(summary = "查询进行中的迭代", description = "查询指定项目中正在进行的迭代")
    @GetMapping("/project/{projectId}/active")
    public Result<List<Iteration>> getActiveIterations(@Parameter(description = "项目ID") @PathVariable Long projectId) {
        Long currentUserId = permissionService.getCurrentUserId();
        List<Iteration> iterations = iterationService.getActiveIterations(projectId, currentUserId);
        return Result.success(iterations);
    }

    @Operation(summary = "查询迭代详情", description = "根据ID查询迭代详细信息")
    @GetMapping("/{id}")
    public Result<Iteration> getIterationById(@Parameter(description = "迭代ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        Iteration iteration = iterationService.getIterationById(id, currentUserId);
        return Result.success(iteration);
    }

    @Operation(summary = "创建迭代", description = "创建新迭代")
    @PostMapping
    @RequirePermission(roles = {1, 4})
    public Result<?> createIteration(@RequestBody Iteration iteration) {
        Long currentUserId = permissionService.getCurrentUserId();
        iterationService.createIteration(iteration, currentUserId);
        return Result.success();
    }

    @Operation(summary = "更新迭代", description = "更新迭代信息")
    @PutMapping("/{id}")
    @RequirePermission(roles = {1, 4})
    public Result<?> updateIteration(@Parameter(description = "迭代ID") @PathVariable Long id,
                                      @RequestBody Iteration iteration) {
        Long currentUserId = permissionService.getCurrentUserId();
        iterationService.updateIteration(id, iteration, currentUserId);
        return Result.success();
    }

    @Operation(summary = "删除迭代", description = "删除迭代")
    @DeleteMapping("/{id}")
    @RequirePermission(roles = {1, 4})
    public Result<?> deleteIteration(@Parameter(description = "迭代ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        iterationService.deleteIteration(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "开始迭代", description = "将迭代状态改为进行中")
    @PostMapping("/{id}/start")
    @RequirePermission(roles = {1, 4})
    public Result<?> startIteration(@Parameter(description = "迭代ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        iterationService.startIteration(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "结束迭代", description = "将迭代状态改为已结束")
    @PostMapping("/{id}/end")
    @RequirePermission(roles = {1, 4})
    public Result<?> endIteration(@Parameter(description = "迭代ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        iterationService.endIteration(id, currentUserId);
        return Result.success();
    }
}
