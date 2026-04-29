package com.demand.module.demand.controller;

import com.demand.common.Result;
import com.demand.module.demand.entity.DemandDependency;
import com.demand.module.demand.service.DemandDependencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "需求依赖管理", description = "需求依赖关系的增删改查接口")
@RestController
@RequestMapping("/api/demand/dependency")
@RequiredArgsConstructor
public class DemandDependencyController {

    private final DemandDependencyService dependencyService;

    @Operation(summary = "查询需求的依赖", description = "查询指定需求依赖的其他需求")
    @GetMapping("/{demandId}/dependencies")
    public Result<List<DemandDependency>> getDependencies(@PathVariable Long demandId) {
        List<DemandDependency> dependencies = dependencyService.getDependenciesByDemandId(demandId);
        return Result.success(dependencies);
    }

    @Operation(summary = "查询依赖此需求的列表", description = "查询哪些需求依赖当前需求")
    @GetMapping("/{demandId}/dependents")
    public Result<List<DemandDependency>> getDependentDemands(@PathVariable Long demandId) {
        List<DemandDependency> dependents = dependencyService.getDependentDemands(demandId);
        return Result.success(dependents);
    }

    @Operation(summary = "添加依赖关系", description = "为需求添加依赖关系")
    @PostMapping
    public Result<?> addDependency(@RequestBody Map<String, Object> params) {
        Long demandId = Long.valueOf(params.get("demandId").toString());
        Long dependsOnId = Long.valueOf(params.get("dependsOnId").toString());
        String dependencyType = params.containsKey("dependencyType") ?
                params.get("dependencyType").toString() : "BLOCKS";
        dependencyService.addDependency(demandId, dependsOnId, dependencyType);
        return Result.success();
    }

    @Operation(summary = "移除依赖关系", description = "移除需求的依赖关系")
    @DeleteMapping("/{demandId}/{dependsOnId}")
    public Result<?> removeDependency(@PathVariable Long demandId, @PathVariable Long dependsOnId) {
        dependencyService.removeDependency(demandId, dependsOnId);
        return Result.success();
    }
}
