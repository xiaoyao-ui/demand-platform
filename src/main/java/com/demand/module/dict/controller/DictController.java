package com.demand.module.dict.controller;

import com.demand.common.Result;
import com.demand.module.dict.entity.Dict;
import com.demand.module.dict.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典控制器
 * <p>
 * 提供数据字典的查询和缓存管理功能。
 * 数据字典用于存储系统中的枚举值和配置项，如需求类型、优先级、状态等。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>字典查询</b>：根据类型查询字典列表（带 Redis 缓存）</li>
 *   <li><b>类型管理</b>：获取所有可用的字典类型</li>
 *   <li><b>缓存刷新</b>：手动刷新指定类型的字典缓存</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>前端下拉框选项：如需求类型、优先级选择器</li>
 *   <li>状态映射：将数字状态码转换为中文描述</li>
 *   <li>系统配置：存储可动态调整的配置项</li>
 * </ul>
 * 
 * <h3>缓存策略：</h3>
 * <p>
 * 字典数据使用 Redis 缓存，有效期 24 小时。
 * 修改字典数据后需调用 {@code /api/dict/refresh/{type}} 刷新缓存。
 * </p>
 */
@Tag(name = "数据字典", description = "数据字典管理接口")
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    /**
     * 数据字典业务逻辑服务
     */
    private final DictService dictService;

    /**
     * 根据类型查询字典列表
     * <p>
     * 从 Redis 缓存中获取字典数据，如果缓存不存在则查询数据库并写入缓存。
     * </p>
     * 
     * <p>
     * <b>使用示例</b>：
     * <ul>
     *   <li>{@code GET /api/dict/demand_type} → 查询需求类型字典</li>
     *   <li>{@code GET /api/dict/priority} → 查询优先级字典</li>
     *   <li>{@code GET /api/dict/demand_status} → 查询需求状态字典</li>
     * </ul>
     * </p>
     *
     * @param type 字典类型（如 demand_type、priority、demand_status）
     * @return 字典列表（按 sort 字段升序排列）
     */
    @Operation(summary = "查询字典列表", description = "根据类型查询字典列表")
    @GetMapping("/{type}")
    public Result<List<Dict>> getDictByType(
            @Parameter(description = "字典类型") @PathVariable String type) {
        List<Dict> dictList = dictService.getDictByType(type);
        return Result.success(dictList);
    }

    /**
     * 查询所有字典类型
     * <p>
     * 返回系统中所有已启用的字典类型，用于前端动态生成字典管理界面。
     * </p>
     *
     * @return 字典类型列表（去重后的字符串数组）
     */
    @Operation(summary = "查询所有字典类型", description = "获取所有可用的字典类型")
    @GetMapping("/types")
    public Result<List<String>> getAllTypes() {
        List<String> types = dictService.getAllTypes();
        return Result.success(types);
    }

    /**
     * 刷新字典缓存
     * <p>
     * 删除指定类型的 Redis 缓存，并重新从数据库加载最新数据。
     * 适用于后台修改字典数据后同步更新缓存。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>管理员在后台修改了字典项的名称或排序</li>
     *   <li>新增了字典类型或禁用了某个字典项</li>
     * </ul>
     * </p>
     *
     * @param type 字典类型
     * @return 操作结果
     */
    @Operation(summary = "刷新字典缓存", description = "刷新指定类型的字典缓存")
    @PostMapping("/refresh/{type}")
    public Result<?> refreshCache(
            @Parameter(description = "字典类型") @PathVariable String type) {
        dictService.refreshCache(type);
        return Result.success();
    }
}
