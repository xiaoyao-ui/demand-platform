package com.demand.module.user.controller;

import com.demand.common.Result;
import com.demand.config.PermissionContext;
import com.demand.config.RequirePermission;
import com.demand.module.user.entity.Permission;
import com.demand.module.user.mapper.PermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色权限管理控制器
 * <p>
 * 提供角色信息查询、动态菜单生成和角色权限配置功能。
 * </p>
 */
@Slf4j
@Tag(name = "角色权限管理", description = "角色和权限管理接口")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final PermissionContext permissionContext;
    private final PermissionMapper permissionMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取所有角色列表
     */
    @Operation(summary = "获取所有角色列表", description = "获取系统中所有角色信息")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getRoleList() {
        List<Map<String, Object>> roles = new ArrayList<>();

        Map<String, Object> superAdmin = new HashMap<>();
        superAdmin.put("id", 1);
        superAdmin.put("roleName", "超级管理员");
        superAdmin.put("roleKey", "SUPER_ADMIN");
        superAdmin.put("description", "拥有所有权限");
        superAdmin.put("isSystem", true);
        roles.add(superAdmin);

        Map<String, Object> user = new HashMap<>();
        user.put("id", 2);
        user.put("roleName", "普通用户");
        user.put("roleKey", "USER");
        user.put("description", "可以提出需求");
        user.put("isSystem", true);
        roles.add(user);

        Map<String, Object> productManager = new HashMap<>();
        productManager.put("id", 3);
        productManager.put("roleName", "产品经理");
        productManager.put("roleKey", "PRODUCT_MANAGER");
        productManager.put("description", "负责需求管理和审批");
        productManager.put("isSystem", true);
        roles.add(productManager);

        Map<String, Object> projectManager = new HashMap<>();
        projectManager.put("id", 4);
        projectManager.put("roleName", "项目经理");
        projectManager.put("roleKey", "PROJECT_MANAGER");
        projectManager.put("description", "负责项目管理和分配");
        projectManager.put("isSystem", true);
        roles.add(projectManager);

        Map<String, Object> developer = new HashMap<>();
        developer.put("id", 5);
        developer.put("roleName", "开发工程师");
        developer.put("roleKey", "DEVELOPER");
        developer.put("description", "负责需求开发");
        developer.put("isSystem", true);
        roles.add(developer);

        Map<String, Object> implementer = new HashMap<>();
        implementer.put("id", 6);
        implementer.put("roleName", "实施/测试");
        implementer.put("roleKey", "IMPLEMENTER");
        implementer.put("description", "负责测试和验收");
        implementer.put("isSystem", true);
        roles.add(implementer);

        Map<String, Object> guest = new HashMap<>();
        guest.put("id", 7);
        guest.put("roleName", "只读用户");
        guest.put("roleKey", "GUEST");
        guest.put("description", "只能查看需求");
        guest.put("isSystem", true);
        roles.add(guest);

        return Result.success(roles);
    }

    /**
     * 获取指定角色的详细信息
     */
    @Operation(summary = "获取角色详情", description = "获取指定角色的详细信息")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getRoleDetail(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        
        Map<String, Object> role = new HashMap<>();
        role.put("id", id);
        
        switch (id.intValue()) {
            case 1:
                role.put("roleName", "超级管理员");
                role.put("roleKey", "SUPER_ADMIN");
                role.put("description", "拥有所有权限，管理系统所有资源");
                break;
            case 2:
                role.put("roleName", "普通用户");
                role.put("roleKey", "USER");
                role.put("description", "可以提出和管理自己的需求");
                break;
            case 3:
                role.put("roleName", "产品经理");
                role.put("roleKey", "PRODUCT_MANAGER");
                role.put("description", "负责需求管理和审批");
                break;
            case 4:
                role.put("roleName", "项目经理");
                role.put("roleKey", "PROJECT_MANAGER");
                role.put("description", "负责项目管理和需求分配");
                break;
            case 5:
                role.put("roleName", "开发工程师");
                role.put("roleKey", "DEVELOPER");
                role.put("description", "负责需求开发实现");
                break;
            case 6:
                role.put("roleName", "实施/测试");
                role.put("roleKey", "IMPLEMENTER");
                role.put("description", "负责需求测试和验收");
                break;
            case 7:
                role.put("roleName", "只读用户");
                role.put("roleKey", "GUEST");
                role.put("description", "仅可查看信息，无编辑权限");
                break;
            default:
                return Result.error(404, "角色不存在");
        }
        
        // 查询角色的权限列表
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleId(id);
        role.put("permissions", permissions.stream()
                .map(Permission::getPerms)
                .filter(perms -> perms != null && !perms.isEmpty())
                .collect(Collectors.toList()));
        
        // 查询角色的菜单列表
        List<Map<String, Object>> menus = buildMenuTree(permissions);
        role.put("menus", menus);
        
        return Result.success(role);
    }

    /**
     * 获取当前用户可访问的菜单
     */
    @Operation(summary = "获取当前用户可访问的菜单", description = "根据用户角色返回可访问的菜单树")
    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getUserMenus() {
        Long userId = permissionContext.getUserId();
        if (userId == null) {
            log.warn("用户未登录，无法获取菜单");
            return Result.error(401, "用户未登录");
        }

        List<String> userRoles = permissionContext.getRoles();
        log.info("获取用户菜单: userId={}, roles={}", userId, userRoles);

        // 查询用户的所有权限
        List<Permission> permissions = permissionMapper.selectPermissionsByUserId(userId);
        
        // 构建菜单树
        List<Map<String, Object>> menuTree = buildMenuTree(permissions);

        log.info("返回菜单数量: {}, userId={}", menuTree.size(), userId);
        return Result.success(menuTree);
    }

    /**
     * 获取所有权限列表（用于角色授权）
     */
    @Operation(summary = "获取所有权限列表", description = "获取系统中所有权限，用于角色授权配置")
    @RequirePermission(roles = {1})
    @GetMapping("/permissions")
    public Result<List<Map<String, Object>>> getAllPermissions() {
        List<Permission> allPermissions = permissionMapper.selectList(null);
        
        // 构建树形结构
        List<Map<String, Object>> permissionTree = buildPermissionTree(allPermissions);
        
        return Result.success(permissionTree);
    }

    /**
     * 为角色分配权限
     */
    @Operation(summary = "为角色分配权限", description = "为指定角色分配权限列表")
    @RequirePermission(roles = {1})
    @PutMapping("/{id}/permissions")
    public Result<?> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody Map<String, List<Long>> params) {
        
        List<Long> permissionIds = params.get("permissionIds");
        if (permissionIds == null) {
            return Result.error(400, "权限ID列表不能为空");
        }
        
        // 检查是否为系统角色
        if (id <= 7) {
            log.warn("尝试修改系统角色权限: roleId={}", id);
            // 系统角色允许修改，但需要记录日志
        }
        
        try {
            // 删除角色的原有权限
            jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", id);
            
            // 插入新的权限关联
            for (Long permissionId : permissionIds) {
                jdbcTemplate.update(
                    "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                    id, permissionId
                );
            }
            
            log.info("角色权限分配成功: roleId={}, permissionCount={}", id, permissionIds.size());
            return Result.success();
        } catch (Exception e) {
            log.error("角色权限分配失败: roleId={}", id, e);
            return Result.error(500, "权限分配失败: " + e.getMessage());
        }
    }

    /**
     * 构建权限树
     */
    private List<Map<String, Object>> buildPermissionTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 按sort排序
        permissions.sort(Comparator.comparing(Permission::getSort));

        // 构建映射关系
        Map<Long, Map<String, Object>> permissionMap = new LinkedHashMap<>();
        for (Permission perm : permissions) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", perm.getId());
            node.put("parentId", perm.getParentId());
            node.put("name", perm.getName());
            node.put("type", perm.getType());
            node.put("path", perm.getPath());
            node.put("perms", perm.getPerms());
            node.put("icon", perm.getIcon());
            node.put("sort", perm.getSort());
            node.put("children", new ArrayList<Map<String, Object>>());
            permissionMap.put(perm.getId(), node);
        }

        // 构建树形结构
        List<Map<String, Object>> rootNodes = new ArrayList<>();
        for (Map<String, Object> node : permissionMap.values()) {
            Long parentId = ((Number) node.get("parentId")).longValue();
            if (parentId == 0) {
                rootNodes.add(node);
            } else {
                Map<String, Object> parent = permissionMap.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                }
            }
        }

        return rootNodes;
    }

    /**
     * 构建菜单树
     */
    private List<Map<String, Object>> buildMenuTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤出菜单和目录（type=1或type=2）
        List<Permission> menuPermissions = permissions.stream()
                .filter(p -> p.getType() == 1 || p.getType() == 2)
                .sorted(Comparator.comparing(Permission::getSort))
                .collect(Collectors.toList());

        // 构建映射关系
        Map<Long, Map<String, Object>> menuMap = new LinkedHashMap<>();
        for (Permission perm : menuPermissions) {
            Map<String, Object> menu = new HashMap<>();
            menu.put("id", perm.getId());
            menu.put("parentId", perm.getParentId());
            menu.put("path", perm.getPath());
            menu.put("name", generateRouteName(perm.getPath(), perm.getName()));
            menu.put("component", generateComponentPath(perm.getPath(), perm.getType()));
            menu.put("redirect", perm.getParentId() == 0 ? generateRedirect(perm.getPath()) : null);
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("title", perm.getName());
            meta.put("icon", perm.getIcon() != null ? perm.getIcon().replace("el-icon-", "") : "");
            meta.put("perms", perm.getPerms());
            menu.put("meta", meta);
            
            menu.put("children", new ArrayList<Map<String, Object>>());
            menuMap.put(perm.getId(), menu);
        }

        // 构建树形结构
        List<Map<String, Object>> rootMenus = new ArrayList<>();
        for (Map<String, Object> menu : menuMap.values()) {
            Long parentId = ((Number) menu.get("parentId")).longValue();
            if (parentId == 0) {
                rootMenus.add(menu);
            } else {
                Map<String, Object> parent = menuMap.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(menu);
                }
            }
        }

        // 清理空的children和内部字段
        cleanMenuTree(rootMenus);
        
        return rootMenus;
    }

    /**
     * 清理菜单树
     */
    private void cleanMenuTree(List<Map<String, Object>> menus) {
        for (Map<String, Object> menu : menus) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) menu.get("children");
            if (children != null && !children.isEmpty()) {
                cleanMenuTree(children);
            } else {
                menu.remove("children");
            }
            menu.remove("parentId");
            menu.remove("id");
        }
    }

    /**
     * 生成路由名称
     */
    private String generateRouteName(String path, String name) {
        if (path == null || path.isEmpty()) {
            return "Unknown";
        }
        String[] parts = path.replaceAll("^/", "").split("/");
        StringBuilder routeName = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                routeName.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    routeName.append(part.substring(1));
                }
            }
        }
        return routeName.toString();
    }

    /**
     * 生成组件路径
     */
    private String generateComponentPath(String path, Integer type) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        if (type == 1) {
            return "Layout";
        }
        
        String cleanPath = path.replaceAll("^/", "");
        String[] parts = cleanPath.split("/");
        
        if (parts.length == 1) {
            return "views/" + parts[0] + "/index.vue";
        } else {
            return "views/" + String.join("/", parts) + ".vue";
        }
    }

    /**
     * 生成重定向路径
     */
    private String generateRedirect(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path + "/" + path.replaceAll(".*/", "");
    }
}
