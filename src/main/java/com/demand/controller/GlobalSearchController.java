package com.demand.controller;

import com.demand.common.Result;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.project.entity.Project;
import com.demand.module.project.mapper.ProjectMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.RoleMapper;
import com.demand.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局搜索控制器
 * <p>
 * 提供跨模块的统一搜索功能，支持同时检索需求、用户、项目等核心业务数据。
 * 适用于顶部导航栏的快速搜索场景，帮助用户快速定位目标资源。
 * </p>
 * 
 * <h3>支持的搜索类型：</h3>
 * <ul>
 *   <li><b>需求搜索</b>：匹配标题、描述、所属模块（最多返回 5 条）</li>
 *   <li><b>用户搜索</b>：匹配真实姓名、邮箱、手机号（最多返回 5 条）</li>
 *   <li><b>项目搜索</b>：匹配项目名称、项目描述（最多返回 5 条）</li>
 * </ul>
 * 
 * <h3>响应格式示例：</h3>
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {
 *     "demands": [
 *       {
 *         "id": 123,
 *         "title": "优化登录页面性能",
 *         "type": "FEATURE",
 *         "moduleName": "用户中心",
 *         "status": "APPROVED",
 *         "category": "demand"
 *       }
 *     ],
 *     "users": [
 *       {
 *         "id": 456,
 *         "name": "张三",
 *         "email": "zhangsan@example.com",
 *         "roles": ["DEVELOPER"],
 *         "category": "user"
 *       }
 *     ],
 *     "projects": [
 *       {
 *         "id": 789,
 *         "name": "电商平台重构",
 *         "description": "对现有电商平台进行全面重构",
 *         "status": "IN_PROGRESS",
 *         "category": "project"
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 * 
 * <p>
 * <b>性能优化建议：</b>
 * <ul>
 *   <li>当前实现为内存过滤（{@code selectAll()} + Stream），数据量大时建议改为数据库 {@code LIKE} 查询</li>
 *   <li>可引入 Elasticsearch 或 Redis Search 实现全文检索和拼音搜索</li>
 *   <li>前端应添加防抖（Debounce），避免每次按键都触发请求</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "全局搜索", description = "支持需求、用户、项目快速搜索")
public class GlobalSearchController {

    /**
     * 需求数据访问层
     */
    private final DemandMapper demandMapper;

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * 角色数据访问层
     */
    private final RoleMapper roleMapper;

    /**
     * 项目数据访问层
     */
    private final ProjectMapper projectMapper;

    /**
     * 执行全局搜索
     * <p>
     * 工作流程：
     * 1. 校验关键词是否为空，为空则返回空结果集
     * 2. 并行搜索需求、用户、项目数据
     * 3. 每种类型最多返回 5 条结果
     * 4. 将结果封装为 Map 结构返回
     * </p>
     * 
     * <p>
     * <b>搜索规则：</b>
     * <ul>
     *   <li><b>需求匹配</b>：标题 OR 描述 OR 所属模块包含关键词</li>
     *   <li><b>用户匹配</b>：真实姓名 OR 邮箱 OR 手机号包含关键词</li>
     *   <li><b>项目匹配</b>：项目名称 OR 项目描述包含关键词</li>
     *   <li><b>大小写不敏感</b>：使用 {@code toLowerCase()} 进行匹配</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>安全提示：</b>
     * <ul>
     *   <li>用户搜索结果中包含邮箱和手机号，建议根据当前用户角色进行权限控制</li>
     *   <li>敏感字段（如手机号）可使用 {@link com.demand.util.PhoneSerializer} 脱敏</li>
     * </ul>
     * </p>
     *
     * @param keyword 搜索关键词（支持模糊匹配）
     * @return 包含需求、用户、项目搜索结果的 Map 对象
     */
    @Operation(summary = "全局搜索", description = "搜索需求、用户、项目数据")
    @GetMapping("/global")
    public Result<Map<String, List<Map<String, Object>>>> globalSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        
        log.debug("执行全局搜索: keyword={}", keyword);
        
        // 1. 校验关键词有效性
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(buildEmptyResult());
        }

        String kw = keyword.trim().toLowerCase();
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // 2. 并行搜索各类型数据
        result.put("demands", searchDemands(kw));
        result.put("users", searchUsers(kw));
        result.put("projects", searchProjects(kw));

        log.debug("全局搜索完成: demands={}, users={}, projects={}", 
                result.get("demands").size(), 
                result.get("users").size(), 
                result.get("projects").size());

        return Result.success(result);
    }

    /**
     * 搜索需求
     *
     * @param keyword 搜索关键词（小写）
     * @return 匹配的需求列表
     */
    private List<Map<String, Object>> searchDemands(String keyword) {
        try {
            List<Demand> demands = demandMapper.selectAll();
            
            return demands.stream()
                    .filter(d -> matchesDemand(d, keyword))
                    .limit(5)
                    .map(this::convertDemandToMap)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("搜索需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查需求是否匹配关键词
     *
     * @param demand 需求对象
     * @param keyword 搜索关键词（小写）
     * @return 是否匹配
     */
    private boolean matchesDemand(Demand demand, String keyword) {
        String title = demand.getTitle() != null ? demand.getTitle().toLowerCase() : "";
        String description = demand.getDescription() != null ? demand.getDescription().toLowerCase() : "";
        String moduleName = demand.getModuleName() != null ? demand.getModuleName().toLowerCase() : "";
        
        return title.contains(keyword) 
            || description.contains(keyword) 
            || moduleName.contains(keyword);
    }

    /**
     * 转换需求为搜索结果 Map
     *
     * @param demand 需求对象
     * @return 搜索结果 Map
     */
    private Map<String, Object> convertDemandToMap(Demand demand) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", demand.getId());
        map.put("title", demand.getTitle());
        map.put("type", demand.getType());
        map.put("moduleName", demand.getModuleName());
        map.put("status", demand.getStatus());
        map.put("creatorName", demand.getCreatorName());
        map.put("category", "demand");
        return map;
    }

    /**
     * 搜索用户
     *
     * @param keyword 搜索关键词（小写）
     * @return 匹配的用户列表
     */
    private List<Map<String, Object>> searchUsers(String keyword) {
        try {
            List<User> users = userMapper.selectAllUsers();
            
            return users.stream()
                    .filter(u -> matchesUser(u, keyword))
                    .limit(5)
                    .map(this::convertUserToMap)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查用户是否匹配关键词
     *
     * @param user 用户对象
     * @param keyword 搜索关键词（小写）
     * @return 是否匹配
     */
    private boolean matchesUser(User user, String keyword) {
        String realName = user.getRealName() != null ? user.getRealName().toLowerCase() : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        String phone = user.getPhone() != null ? user.getPhone().toLowerCase() : "";
        
        return realName.contains(keyword) 
            || email.contains(keyword) 
            || phone.contains(keyword);
    }

    /**
     * 转换用户为搜索结果 Map
     *
     * @param user 用户对象
     * @return 搜索结果 Map
     */
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getRealName());
        map.put("email", user.getEmail());
        
        // 获取用户角色
        try {
            List<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());
            map.put("roles", roles != null ? roles : Collections.emptyList());
        } catch (Exception e) {
            log.warn("获取用户角色失败: userId={}", user.getId(), e);
            map.put("roles", Collections.emptyList());
        }
        
        map.put("category", "user");
        return map;
    }

    /**
     * 搜索项目
     *
     * @param keyword 搜索关键词（小写）
     * @return 匹配的项目列表
     */
    private List<Map<String, Object>> searchProjects(String keyword) {
        try {
            List<Project> projects = projectMapper.selectAll();
            
            return projects.stream()
                    .filter(p -> matchesProject(p, keyword))
                    .limit(5)
                    .map(this::convertProjectToMap)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("搜索项目失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查项目是否匹配关键词
     *
     * @param project 项目对象
     * @param keyword 搜索关键词（小写）
     * @return 是否匹配
     */
    private boolean matchesProject(Project project, String keyword) {
        String name = project.getName() != null ? project.getName().toLowerCase() : "";
        String description = project.getDescription() != null ? project.getDescription().toLowerCase() : "";
        
        return name.contains(keyword) || description.contains(keyword);
    }

    /**
     * 转换项目为搜索结果 Map
     *
     * @param project 项目对象
     * @return 搜索结果 Map
     */
    private Map<String, Object> convertProjectToMap(Project project) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", project.getId());
        map.put("name", project.getName());
        map.put("description", project.getDescription());
        map.put("status", project.getStatus());
        map.put("managerName", project.getManagerName());
        map.put("category", "project");
        return map;
    }

    /**
     * 构建空结果集
     *
     * @return 空的搜索结果 Map
     */
    private Map<String, List<Map<String, Object>>> buildEmptyResult() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("demands", Collections.emptyList());
        result.put("users", Collections.emptyList());
        result.put("projects", Collections.emptyList());
        return result;
    }
}
