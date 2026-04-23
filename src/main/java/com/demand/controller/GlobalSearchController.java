package com.demand.controller;

import com.demand.common.Result;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.module.entity.Module;
import com.demand.module.module.mapper.ModuleMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局搜索控制器
 * <p>
 * 提供跨模块的统一搜索功能，支持同时检索需求、用户等核心业务数据。
 * 适用于顶部导航栏的快速搜索场景，帮助用户快速定位目标资源。
 * </p>
 * 
 * <h3>支持的搜索类型：</h3>
 * <ul>
 *   <li><b>需求搜索</b>：匹配标题、描述、所属模块（最多返回 5 条）</li>
 *   <li><b>用户搜索</b>：匹配真实姓名、邮箱、手机号（最多返回 5 条）</li>
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
 *         "type": 1,
 *         "module": "用户中心",
 *         "status": 2,
 *         "category": "demand"
 *       }
 *     ],
 *     "users": [
 *       {
 *         "id": 456,
 *         "name": "张三",
 *         "email": "zhangsan@example.com",
 *         "role": 1,
 *         "category": "user"
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
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "全局搜索", description = "支持需求、用户、模块快速搜索")
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
     * 执行全局搜索
     * <p>
     * 工作流程：
     * 1. 校验关键词是否为空，为空则返回空结果集
     * 2. 从数据库加载所有需求和用户数据
     * 3. 使用 Stream API 进行内存过滤（匹配标题、描述、姓名等字段）
     * 4. 每种类型最多返回 5 条结果（通过 {@code limit(5)} 限制）
     * 5. 将结果封装为 Map 结构返回
     * </p>
     * 
     * <p>
     * <b>搜索规则：</b>
     * <ul>
     *   <li><b>需求匹配</b>：标题 OR 描述 OR 所属模块包含关键词</li>
     *   <li><b>用户匹配</b>：真实姓名 OR 邮箱 OR 手机号包含关键词</li>
     *   <li><b>大小写敏感</b>：当前使用 {@code String.contains()}，区分大小写</li>
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
     * @return 包含需求和用户搜索结果的 Map 对象
     */
    @Operation(summary = "全局搜索", description = "搜索需求、用户数据")
    @GetMapping("/global")
    public Result<Map<String, List<Map<String, Object>>>> globalSearch(@RequestParam String keyword) {
        // 1. 校验关键词有效性
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(Map.of(
                "demands", Collections.emptyList(),
                "users", Collections.emptyList()
            ));
        }

        String kw = keyword.trim();
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        // 2. 搜索需求（匹配标题、描述、模块）
        List<Demand> demands = demandMapper.selectAll();
        List<Map<String, Object>> demandResults = demands.stream()
                .filter(d -> d.getTitle().contains(kw) 
                          || (d.getDescription() != null && d.getDescription().contains(kw))
                          || (d.getModule() != null && d.getModule().contains(kw)))
                .limit(5)  // 最多返回 5 条
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("title", d.getTitle());
                    map.put("type", d.getType());
                    map.put("module", d.getModule());
                    map.put("status", d.getStatus());
                    map.put("category", "demand");  // 标识数据类型
                    return map;
                })
                .collect(Collectors.toList());
        result.put("demands", demandResults);

        // 3. 搜索用户（匹配姓名、邮箱、手机号）
        List<User> users = userMapper.selectAllUsers();
        List<Map<String, Object>> userResults = users.stream()
                .filter(u -> u.getRealName().contains(kw) 
                          || (u.getEmail() != null && u.getEmail().contains(kw))
                          || (u.getPhone() != null && u.getPhone().contains(kw)))
                .limit(5)  // 最多返回 5 条
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getRealName());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole());
                    map.put("category", "user");  // 标识数据类型
                    return map;
                })
                .collect(Collectors.toList());
        result.put("users", userResults);

        return Result.success(result);
    }
}
