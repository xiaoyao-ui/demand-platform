package com.demand.module.demand.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.demand.entity.DemandTag;
import com.demand.module.demand.service.DemandTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "需求标签管理", description = "标签的增删改查接口")
@RestController
@RequestMapping("/api/demand/tag")
@RequiredArgsConstructor
public class DemandTagController {

    private final DemandTagService demandTagService;

    @Operation(summary = "查询所有标签", description = "按使用次数降序返回所有标签")
    @GetMapping("/list")
    public Result<List<DemandTag>> getAllTags() {
        List<DemandTag> tags = demandTagService.getAllTags();
        return Result.success(tags);
    }

    @Operation(summary = "按分类查询标签", description = "查询指定分类的标签")
    @GetMapping("/category/{category}")
    public Result<List<DemandTag>> getTagsByCategory(@PathVariable String category) {
        List<DemandTag> tags = demandTagService.getTagsByCategory(category);
        return Result.success(tags);
    }

    @Operation(summary = "查询需求的标签", description = "查询指定需求关联的标签")
    @GetMapping("/demand/{demandId}")
    public Result<List<DemandTag>> getTagsByDemandId(@PathVariable Long demandId) {
        List<DemandTag> tags = demandTagService.getTagsByDemandId(demandId);
        return Result.success(tags);
    }

    @Operation(summary = "创建标签", description = "创建新标签")
    @PostMapping
    @RequirePermission(roles = {1})
    public Result<?> createTag(@RequestBody DemandTag tag) {
        demandTagService.createTag(tag);
        return Result.success();
    }

    @Operation(summary = "更新标签", description = "更新标签信息")
    @PutMapping("/{id}")
    @RequirePermission(roles = {1})
    public Result<?> updateTag(@PathVariable Long id, @RequestBody DemandTag tag) {
        demandTagService.updateTag(id, tag);
        return Result.success();
    }

    @Operation(summary = "删除标签", description = "删除标签（系统内置标签不可删除）")
    @DeleteMapping("/{id}")
    @RequirePermission(roles = {1})
    public Result<?> deleteTag(@PathVariable Long id) {
        demandTagService.deleteTag(id);
        return Result.success();
    }

    @Operation(summary = "为需求添加标签", description = "批量为需求添加标签")
    @PostMapping("/demand/{demandId}/add")
    public Result<?> addTagsToDemand(@PathVariable Long demandId, @RequestBody List<Long> tagIds) {
        demandTagService.addTagsToDemand(demandId, tagIds);
        return Result.success();
    }

    @Operation(summary = "从需求移除标签", description = "批量从需求移除标签")
    @PostMapping("/demand/{demandId}/remove")
    public Result<?> removeTagsFromDemand(@PathVariable Long demandId, @RequestBody List<Long> tagIds) {
        demandTagService.removeTagsFromDemand(demandId, tagIds);
        return Result.success();
    }
}
