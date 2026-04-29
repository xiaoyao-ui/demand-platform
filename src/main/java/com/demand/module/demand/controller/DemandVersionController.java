package com.demand.module.demand.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.demand.dto.DemandVersionDTO;
import com.demand.module.demand.service.DemandService;
import com.demand.module.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 需求版本管理控制器
 */
@Tag(name = "需求版本管理", description = "需求版本查询和回滚接口")
@RestController
@RequestMapping("/api/demand/{demandId}/versions")
@RequiredArgsConstructor
public class DemandVersionController {

    private final DemandService demandService;
    private final PermissionService permissionService;

    /**
     * 获取需求版本历史
     */
    @Operation(summary = "获取版本历史", description = "查询需求的所有版本快照")
    @GetMapping
    public Result<List<DemandVersionDTO>> getVersionHistory(
            @Parameter(description = "需求ID") @PathVariable Long demandId) {
        List<DemandVersionDTO> versions = demandService.getDemandVersions(demandId);
        return Result.success(versions);
    }

    /**
     * 回滚到指定版本
     */
    @Operation(summary = "回滚版本", description = "将需求恢复到指定版本的状态")
    @PostMapping("/{versionNumber}/rollback")
    @RequirePermission(roles = {1, 2, 3, 4})
    public Result<?> rollbackToVersion(
            @Parameter(description = "需求ID") @PathVariable Long demandId,
            @Parameter(description = "版本号") @PathVariable Integer versionNumber) {
        Long operatorId = permissionService.getCurrentUserId();
        Integer newVersion = demandService.rollbackDemandVersion(demandId, versionNumber, operatorId);
        return Result.success(Map.of("newVersion", newVersion, "message", "回滚成功"));
    }
}
