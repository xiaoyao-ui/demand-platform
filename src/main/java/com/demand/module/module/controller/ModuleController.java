package com.demand.module.module.controller;

import com.demand.common.Result;
import com.demand.module.module.entity.Module;
import com.demand.module.module.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块管理控制器
 * <p>
 * 提供系统功能模块的增删改查功能。
 * 模块用于标识和管理系统的不同功能区域，如"需求管理"、"用户管理"、"统计分析"等。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>模块查询</b>：查询所有启用的模块或全部模块（含禁用）</li>
 *   <li><b>模块创建</b>：新增功能模块，自动设置状态为启用</li>
 *   <li><b>模块更新</b>：修改模块信息，检查编码唯一性</li>
 *   <li><b>模块删除</b>：物理删除模块记录</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>前端动态菜单：根据启用的模块生成导航栏</li>
 *   <li>权限控制：限制某些角色只能访问特定模块</li>
 *   <li>功能开关：通过禁用模块临时关闭某项功能</li>
 * </ul>
 * 
 * <h3>缓存策略：</h3>
 * <p>
 * 启用模块列表使用 Spring Cache 缓存（Key: {@code modules::enabled}），
 * 修改模块数据后自动清除缓存，确保数据一致性。
 * </p>
 */
@RestController
@RequestMapping("/api/module")
@RequiredArgsConstructor
@Tag(name = "模块管理", description = "模块的增删改查")
public class ModuleController {

    /**
     * 模块业务逻辑服务
     */
    private final ModuleService moduleService;

    /**
     * 查询所有启用的模块
     * <p>
     * 返回状态为启用（status=1）的模块列表，按排序号升序排列。
     * 数据来自 Redis 缓存，首次查询后后续请求直接从缓存读取。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>前端登录后获取可访问的功能模块</li>
     *   <li>动态生成侧边栏导航菜单</li>
     * </ul>
     * </p>
     *
     * @return 启用的模块列表
     */
    @Operation(summary = "查询所有启用的模块", description = "返回所有状态为启用的模块列表")
    @GetMapping("/list")
    public Result<List<Module>> listEnabled() {
        return Result.success(moduleService.listEnabledModules());
    }

    /**
     * 查询所有模块（含禁用）
     * <p>
     * 返回系统中的所有模块，包括已禁用的模块。
     * 主要用于后台管理界面展示完整的模块列表。
     * </p>
     *
     * @return 所有模块列表
     */
    @Operation(summary = "查询所有模块", description = "返回所有模块列表（含禁用）")
    @GetMapping("/all")
    public Result<List<Module>> listAll() {
        return Result.success(moduleService.getAllModules());
    }

    /**
     * 创建模块
     * <p>
     * 新增一个功能模块，自动设置状态为启用（status=1）。
     * 会检查模块编码的唯一性，防止重复创建。
     * </p>
     * 
     * <p>
     * <b>必填字段</b>：
     * <ul>
     *   <li>{@code name} - 模块名称（如"需求管理"）</li>
     *   <li>{@code code} - 模块编码（如"demand"，必须唯一）</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>可选字段</b>：
     * <ul>
     *   <li>{@code description} - 模块描述</li>
     *   <li>{@code sort} - 排序号（数值越小越靠前）</li>
     * </ul>
     * </p>
     *
     * @param module 模块对象
     * @return 操作结果
     * @throws BusinessException 当模块编码已存在时抛出
     */
    @Operation(summary = "创建模块", description = "新增一个模块")
    @PostMapping
    public Result<Void> create(@RequestBody Module module) {
        moduleService.createModule(module);
        return Result.success();
    }

    /**
     * 更新模块信息
     * <p>
     * 修改模块的名称、描述、排序号、状态等字段。
     * 如果修改了模块编码，会检查新编码是否已被其他模块使用。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>修改模块名称或描述</li>
     *   <li>调整模块在菜单中的显示顺序</li>
     *   <li>禁用/启用某个模块</li>
     * </ul>
     * </p>
     *
     * @param id     模块 ID
     * @param module 更新后的模块对象
     * @return 操作结果
     * @throws BusinessException 当模块不存在或编码已被使用时抛出
     */
    @Operation(summary = "更新模块", description = "更新模块信息")
    @PutMapping("/{id}")
    public Result<?> updateModule(@PathVariable Long id, @RequestBody Module module) {
        moduleService.updateModule(id, module);
        return Result.success();
    }

    /**
     * 删除模块
     * <p>
     * 物理删除模块记录，同时清除相关的 Redis 缓存。
     * 删除前会检查模块是否存在。
     * </p>
     * 
     * <p>
     * <b>注意</b>：删除操作不可恢复，请谨慎使用！
     * </p>
     *
     * @param id 模块 ID
     * @return 操作结果
     * @throws BusinessException 当模块不存在时抛出
     */
    @Operation(summary = "删除模块", description = "删除指定模块")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        moduleService.deleteModule(id);
        return Result.success();
    }
}
