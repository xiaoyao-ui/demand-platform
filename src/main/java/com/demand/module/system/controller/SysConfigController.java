package com.demand.module.system.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.system.entity.SysConfig;
import com.demand.module.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "系统配置", description = "系统配置管理接口（仅管理员）")
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "获取所有配置", description = "获取所有系统配置（按分组）")
    @GetMapping
    @RequirePermission(roles = {1})
    public Result<Map<String, List<SysConfig>>> getAllConfigs() {
        Map<String, List<SysConfig>> configs = sysConfigService.getAllConfigs();
        return Result.success(configs);
    }

    @Operation(summary = "根据分组获取配置", description = "获取指定分组的配置列表")
    @GetMapping("/group/{configGroup}")
    @RequirePermission(roles = {1})
    public Result<List<SysConfig>> getConfigsByGroup(
            @Parameter(description = "配置分组") @PathVariable String configGroup) {
        List<SysConfig> configs = sysConfigService.getConfigsByGroup(configGroup);
        return Result.success(configs);
    }

    @Operation(summary = "获取配置值", description = "根据配置键获取配置值")
    @GetMapping("/value/{configKey}")
    public Result<String> getConfigValue(
            @Parameter(description = "配置键") @PathVariable String configKey) {
        String value = sysConfigService.getConfigValue(configKey);
        return Result.success(value);
    }

    @Operation(summary = "更新配置", description = "更新单个配置项")
    @PutMapping("/{id}")
    @RequirePermission(roles = {1})
    public Result<?> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @RequestBody SysConfig config) {
        sysConfigService.updateConfig(id, config);
        return Result.success();
    }

    @Operation(summary = "批量更新配置", description = "批量更新多个配置项")
    @PutMapping("/batch")
    @RequirePermission(roles = {1})
    public Result<?> batchUpdateConfigs(@RequestBody List<SysConfig> configs) {
        sysConfigService.batchUpdateConfigs(configs);
        return Result.success();
    }

    @Operation(summary = "刷新配置缓存", description = "清除并重新加载配置缓存")
    @PostMapping("/refresh/{configKey}")
    @RequirePermission(roles = {1})
    public Result<?> refreshCache(
            @Parameter(description = "配置键") @PathVariable String configKey) {
        sysConfigService.refreshCache(configKey);
        return Result.success();
    }
}
