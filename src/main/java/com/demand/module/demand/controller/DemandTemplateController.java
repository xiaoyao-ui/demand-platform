package com.demand.module.demand.controller;

import com.demand.common.Result;
import com.demand.module.demand.entity.DemandTemplate;
import com.demand.module.demand.service.DemandTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 需求模板管理控制器
 * <p>
 * 提供需求模板的增删改查功能。
 * 模板用于标准化需求的创建流程，提高需求描述的规范性和完整性。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>模板查询</b>：查询所有启用的模板（用于创建需求时选择）或全部模板（用于管理）</li>
 *   <li><b>模板创建</b>：新增需求模板，定义标准的需求描述结构</li>
 *   <li><b>模板更新</b>：修改模板内容、排序或启用状态</li>
 *   <li><b>模板删除</b>：删除不再使用的模板</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li><b>功能需求模板</b>：包含背景、目标、功能列表、验收标准等字段</li>
 *   <li><b>Bug修复模板</b>：包含问题描述、复现步骤、预期结果、实际结果等字段</li>
 *   <li><b>性能优化模板</b>：包含当前性能指标、优化目标、测试方案等字段</li>
 * </ul>
 * 
 * <h3>使用流程：</h3>
 * <ol>
 *   <li>管理员在后台创建多个需求模板</li>
 *   <li>用户创建需求时，从下拉框选择合适的模板</li>
 *   <li>系统自动填充模板内容到需求描述编辑器</li>
 *   <li>用户根据模板结构填写具体内容</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
@Tag(name = "需求模板管理", description = "需求模板的增删改查接口")
public class DemandTemplateController {

    /**
     * 需求模板业务逻辑服务
     */
    private final DemandTemplateService templateService;

    /**
     * 查询所有启用的模板
     * <p>
     * 返回状态为启用（status=1）的模板列表，按排序号升序排列。
     * 主要用于前端创建需求时的模板选择下拉框。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>用户点击"创建需求"按钮时，调用此接口获取可用模板</li>
     *   <li>用户选择模板后，系统自动将模板内容填充到编辑器</li>
     * </ul>
     * </p>
     *
     * @return 启用的模板列表
     */
    @Operation(summary = "查询所有启用的模板", description = "创建需求时选择模板使用")
    @GetMapping("/list")
    public Result<List<DemandTemplate>> getEnabledTemplates() {
        return Result.success(templateService.getAllEnabled());
    }

    /**
     * 查询所有模板（含禁用）
     * <p>
     * 返回系统中的所有模板，包括已禁用的模板。
     * 主要用于后台管理界面展示完整的模板列表。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>管理员查看和管理所有模板</li>
     *   <li>管理员可以启用/禁用某个模板</li>
     *   <li>管理员可以调整模板的显示顺序</li>
     * </ul>
     * </p>
     *
     * @return 所有模板列表
     */
    @Operation(summary = "查询所有模板", description = "模板管理页面使用")
    @GetMapping("/all")
    public Result<List<DemandTemplate>> getAllTemplates() {
        return Result.success(templateService.getAll());
    }

    /**
     * 创建模板
     * <p>
     * 新增一个需求模板，定义标准的需求描述结构。
     * </p>
     * 
     * <p>
     * <b>必填字段</b>：
     * <ul>
     *   <li>{@code name} - 模板名称（如"功能需求模板"）</li>
     *   <li>{@code content} - 模板内容（Markdown 格式）</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>可选字段</b>：
     * <ul>
     *   <li>{@code description} - 模板说明</li>
     *   <li>{@code sort} - 排序号（数值越小越靠前）</li>
     *   <li>{@code status} - 状态（默认启用，status=1）</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>模板内容示例</b>：
     * <pre>
     * ## 一、需求背景
     * （描述需求的业务背景和痛点）
     * 
     * ## 二、需求目标
     * （描述需求要解决的问题和达到的目标）
     * 
     * ## 三、功能列表
     * 1. 功能点1
     * 2. 功能点2
     * 
     * ## 四、验收标准
     * （描述需求完成的验收条件）
     * </pre>
     * </p>
     *
     * @param template 模板对象
     * @return 操作结果
     */
    @Operation(summary = "创建模板")
    @PostMapping
    public Result<?> createTemplate(@RequestBody DemandTemplate template) {
        templateService.create(template);
        return Result.success();
    }

    /**
     * 更新模板
     * <p>
     * 修改模板的名称、内容、排序号或启用状态。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>优化模板内容，使其更加清晰易懂</li>
     *   <li>调整模板在列表中的显示顺序</li>
     *   <li>临时禁用某个模板（不删除，保留历史数据）</li>
     * </ul>
     * </p>
     *
     * @param id       模板 ID
     * @param template 更新后的模板对象
     * @return 操作结果
     */
    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<?> updateTemplate(@PathVariable Long id, @RequestBody DemandTemplate template) {
        template.setId(id);
        templateService.update(template);
        return Result.success();
    }

    /**
     * 删除模板
     * <p>
     * 物理删除模板记录。
     * 删除后，已使用该模板创建的需求不受影响（模板内容已复制到需求中）。
     * </p>
     * 
     * <p>
     * <b>注意</b>：删除操作不可恢复，建议先禁用模板，确认无人使用后再删除。
     * </p>
     *
     * @param id 模板 ID
     * @return 操作结果
     */
    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<?> deleteTemplate(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }
}
