package com.demand.module.demand.controller;

import com.demand.common.Result;
import com.demand.module.demand.entity.DemandQualityScore;
import com.demand.module.demand.service.DemandQualityScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 需求质量评分控制器
 */
@Tag(name = "需求质量评分", description = "需求质量评估和改进建议")
@RestController
@RequestMapping("/api/demand/{demandId}/quality")
@RequiredArgsConstructor
public class DemandQualityScoreController {

    private final DemandQualityScoreService qualityScoreService;

    /**
     * 获取需求质量评分
     */
    @Operation(summary = "获取质量评分", description = "查看需求的完整性评分和改进建议")
    @GetMapping("/score")
    public Result<DemandQualityScore> getQualityScore(
            @Parameter(description = "需求ID") @PathVariable Long demandId) {
        DemandQualityScore score = qualityScoreService.calculateScore(demandId);
        return Result.success(score);
    }
}
