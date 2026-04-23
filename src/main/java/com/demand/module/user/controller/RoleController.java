package com.demand.module.user.controller;

import com.demand.common.Result;
import com.demand.config.PermissionContext;
import com.demand.config.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 角色权限管理控制器
 * <p>
 * 提供角色信息查询和动态菜单生成功能。
 * 根据用户角色返回可访问的菜单列表，实现前端路由级别的权限控制。
 * </p>
 * 
 * <h3>角色定义：</h3>
 * <ul>
 *   <li><b>0 - 只读用户</b>：仅可查看需求、附件等公开信息</li>
 *   <li><b>1 - 普通用户</b>：可创建、编辑、删除自己的需求</li>
 *   <li><b>2 - 管理员</b>：系统最高权限，可管理用户、角色、日志等</li>
 *   <li><b>3 - 项目经理</b>：可审批需求、分配负责人、查看所有需求</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>前端登录后调用 {@code /api/role/menus} 获取动态菜单</li>
 *   <li>根据返回的菜单列表渲染侧边栏导航</li>
 *   <li>隐藏无权限的按钮和操作入口</li>
 * </ul>
 */
@Slf4j
@Tag(name = "角色权限管理", description = "角色和权限管理接口")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    /**
     * 权限上下文，用于获取当前用户角色
     */
    private final PermissionContext permissionContext;

    /**
     * 获取所有角色列表
     * <p>
     * 返回系统中预定义的 4 种角色及其权限配置，
     * 用于前端展示角色说明或下拉选择。
     * </p>
     *
     * @return 角色列表（包含 code、name、description、permissions、menus）
     */
    @Operation(summary = "获取所有角色列表", description = "获取系统中所有预定义的角色信息")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getRoleList() {
        List<Map<String, Object>> roles = new ArrayList<>();
        
        // 只读用户
        Map<String, Object> readOnly = new HashMap<>();
        readOnly.put("code", 0);
        readOnly.put("name", "只读用户");
        readOnly.put("description", "仅可查看信息，无编辑权限");
        readOnly.put("permissions", Arrays.asList("查看需求", "查看附件", "查看用户"));
        readOnly.put("menus", Arrays.asList("/dashboard", "/demand"));
        roles.add(readOnly);
        
        // 普通用户
        Map<String, Object> user = new HashMap<>();
        user.put("code", 1);
        user.put("name", "普通用户");
        user.put("description", "可创建和管理自己的需求");
        user.put("permissions", Arrays.asList("创建需求", "编辑草稿", "删除草稿", "撤回需求", "查看需求", "查看附件", "评论需求"));
        user.put("menus", Arrays.asList("/dashboard", "/demand"));
        roles.add(user);
        
        // 管理员
        Map<String, Object> admin = new HashMap<>();
        admin.put("code", 2);
        admin.put("name", "管理员");
        admin.put("description", "系统最高权限，管理所有资源");
        admin.put("permissions", Arrays.asList("所有权限", "用户管理", "角色管理", "系统配置", "数据导出", "日志查看"));
        admin.put("menus", Arrays.asList("/dashboard", "/demand", "/user", "/role"));
        roles.add(admin);
        
        // 项目经理
        Map<String, Object> pm = new HashMap<>();
        pm.put("code", 3);
        pm.put("name", "项目经理");
        pm.put("description", "可审批和分配需求");
        pm.put("permissions", Arrays.asList("审批需求", "分配负责人", "查看所有需求", "编辑需求", "查看附件", "评论需求"));
        pm.put("menus", Arrays.asList("/dashboard", "/demand"));
        roles.add(pm);
        
        return Result.success(roles);
    }

    /**
     * 获取指定角色的详细信息
     * <p>
     * 根据角色代码返回该角色的完整配置，包括权限列表和可访问菜单。
     * </p>
     *
     * @param code 角色代码（0-只读，1-普通，2-管理员，3-项目经理）
     * @return 角色详细信息
     */
    @Operation(summary = "获取角色详情", description = "获取指定角色的详细信息")
    @GetMapping("/{code}")
    public Result<Map<String, Object>> getRoleDetail(
            @Parameter(description = "角色代码") @PathVariable Integer code) {
        
        Map<String, Object> role = new HashMap<>();
        
        switch (code) {
            case 0:
                role.put("code", 0);
                role.put("name", "只读用户");
                role.put("description", "仅可查看信息，无编辑权限");
                role.put("permissions", Arrays.asList("查看需求", "查看附件", "查看用户"));
                role.put("menus", Arrays.asList("/dashboard", "/demand"));
                break;
            case 1:
                role.put("code", 1);
                role.put("name", "普通用户");
                role.put("description", "可创建和管理自己的需求");
                role.put("permissions", Arrays.asList("创建需求", "编辑草稿", "删除草稿", "撤回需求", "查看需求", "查看附件", "评论需求"));
                role.put("menus", Arrays.asList("/dashboard", "/demand"));
                break;
            case 2:
                role.put("code", 2);
                role.put("name", "管理员");
                role.put("description", "系统最高权限，管理所有资源");
                role.put("permissions", Arrays.asList("所有权限", "用户管理", "角色管理", "系统配置", "数据导出", "日志查看"));
                role.put("menus", Arrays.asList("/dashboard", "/demand", "/user", "/role"));
                break;
            case 3:
                role.put("code", 3);
                role.put("name", "项目经理");
                role.put("description", "可审批和分配需求");
                role.put("permissions", Arrays.asList("审批需求", "分配负责人", "查看所有需求", "编辑需求", "查看附件", "评论需求"));
                role.put("menus", Arrays.asList("/dashboard", "/demand"));
                break;
            default:
                return Result.error(404, "角色不存在");
        }
        
        return Result.success(role);
    }

    /**
     * 获取当前用户可访问的菜单
     * <p>
     * 根据用户角色动态生成菜单列表，实现前端路由级别的权限控制。
     * 不同角色看到的菜单项不同：
     * <ul>
     *   <li>所有角色：首页、需求管理</li>
     *   <li>管理员额外：用户管理、角色权限管理、操作日志</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>前端使用示例</b>：
     * <pre>{@code
     * // 登录后调用此接口
     * const menus = await getUserMenus()
     * // 根据返回的菜单列表渲染侧边栏
     * menus.forEach(menu => {
     *   addRoute({ path: menu.path, component: ... })
     * })
     * }</pre>
     * </p>
     *
     * @return 菜单列表（包含 path、name、icon、title、roles）
     */
    @Operation(summary = "获取当前用户可访问的菜单", description = "根据用户角色返回可访问的菜单列表")
    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getUserMenus() {
        // 1. 从 JWT 中获取当前用户角色
        Integer userRole = permissionContext.getRole();
        if (userRole == null) {
            log.warn("用户未登录，无法获取菜单");
            return Result.error(401, "用户未登录");
        }
        
        log.info("获取用户菜单: userRole={}", userRole);
        
        // 2. 根据用户角色过滤菜单
        List<Map<String, Object>> menus = new ArrayList<>();
        
        // 首页 - 所有角色可访问
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("path", "/dashboard");
        dashboard.put("name", "Dashboard");
        dashboard.put("icon", "HomeFilled");
        dashboard.put("title", "首页");
        dashboard.put("roles", Arrays.asList(0, 1, 2, 3));
        menus.add(dashboard);
        
        // 需求管理 - 所有角色可访问
        Map<String, Object> demand = new HashMap<>();
        demand.put("path", "/demand");
        demand.put("name", "Demand");
        demand.put("icon", "List");
        demand.put("title", "需求管理");
        demand.put("roles", Arrays.asList(0, 1, 2, 3));
        menus.add(demand);
        
        // 用户管理 - 仅管理员可访问
        if (userRole == 2) {
            Map<String, Object> user = new HashMap<>();
            user.put("path", "/user");
            user.put("name", "User");
            user.put("icon", "UserFilled");
            user.put("title", "用户管理");
            user.put("roles", Arrays.asList(2));
            menus.add(user);
        }
        
        // 角色权限管理 - 仅管理员可访问
        if (userRole == 2) {
            Map<String, Object> role = new HashMap<>();
            role.put("path", "/role");
            role.put("name", "Role");
            role.put("icon", "Management");
            role.put("title", "角色权限管理");
            role.put("roles", Arrays.asList(2));
            menus.add(role);
        }
        
        // 操作日志 - 仅管理员可访问
        if (userRole == 2) {
            Map<String, Object> log = new HashMap<>();
            log.put("path", "/log");
            log.put("name", "Log");
            log.put("icon", "Document");
            log.put("title", "操作日志");
            log.put("roles", Arrays.asList(2));
            menus.add(log);
        }
        
        log.info("返回菜单数量: {}, userRole={}", menus.size(), userRole);
        return Result.success(menus);
    }
}
