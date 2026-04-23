package com.demand.module.user.controller;

import com.demand.common.Result;
import com.demand.config.RateLimit;
import com.demand.config.RequirePermission;
import com.demand.module.user.dto.UserCreateDTO;
import com.demand.module.user.dto.UserEmailLoginDTO;
import com.demand.module.user.dto.UserLoginDTO;
import com.demand.module.user.dto.UserPhoneLoginDTO;
import com.demand.module.user.dto.UserRegisterDTO;
import com.demand.module.user.dto.UserUpdateDTO;
import com.demand.module.user.entity.User;
import com.demand.module.user.service.PermissionService;
import com.demand.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * <p>
 * 提供用户注册、登录、信息查询、权限管理等核心功能。
 * 支持多种登录方式（用户名密码、邮箱验证码、手机验证码）。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>用户认证</b>：注册、三种登录方式、修改密码</li>
 *   <li><b>用户管理</b>：创建用户、更新信息、禁用/启用账号</li>
 *   <li><b>权限控制</b>：基于角色的访问控制（RBAC）</li>
 *   <li><b>安全防护</b>：登录限流（5次/分钟）、密码加密存储</li>
 * </ul>
 * 
 * <h3>角色说明：</h3>
 * <ul>
 *   <li>0 - 只读用户：仅可查看信息，无编辑权限</li>
 *   <li>1 - 普通用户：可创建和管理自己的需求</li>
 *   <li>2 - 管理员：系统最高权限，管理所有资源</li>
 *   <li>3 - 项目经理：可审批和分配需求</li>
 * </ul>
 */
@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录和查询接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    /**
     * 用户业务逻辑服务
     */
    private final UserService userService;

    /**
     * 权限验证服务
     */
    private final PermissionService permissionService;

    /**
     * 用户注册
     * <p>
     * 注册流程：
     * 1. 校验用户名是否已存在
     * 2. 验证邮箱验证码
     * 3. 密码加密后存入数据库
     * 4. 默认分配"普通用户"角色（role=1）
     * </p>
     *
     * @param registerDTO 注册请求对象（包含用户名、密码、邮箱、验证码等）
     * @return 操作结果
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    /**
     * 用户名密码登录
     * <p>
     * 登录流程：
     * 1. 根据用户名查询用户
     * 2. 验证密码是否正确（BCrypt 比对）
     * 3. 检查账号状态（是否被禁用）
     * 4. 生成 JWT Token（包含 userId、username、role）
     * </p>
     * 
     * <p>
     * <b>限流保护</b>：60 秒内最多 5 次尝试，防止暴力破解
     * </p>
     *
     * @param loginDTO 登录请求对象（包含用户名和密码）
     * @return 包含 Token 和用户信息的 Map
     */
    @Operation(summary = "用户名密码登录", description = "用户名密码登录并获取 JWT Token")
    @RateLimit(timeWindow = 60, maxRequests = 5, keyPrefix = "login_limit")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        Map<String, Object> result = userService.login(loginDTO);
        return Result.success(result);
    }

    /**
     * 邮箱验证码登录
     * <p>
     * 登录流程：
     * 1. 验证邮箱验证码是否正确
     * 2. 根据邮箱查询用户
     * 3. 检查账号状态
     * 4. 生成 JWT Token
     * </p>
     * 
     * <p>
     * <b>限流保护</b>：60 秒内最多 5 次尝试
     * </p>
     *
     * @param loginDTO 邮箱登录请求对象（包含邮箱和验证码）
     * @return 包含 Token 和用户信息的 Map
     */
    @Operation(summary = "邮箱验证码登录", description = "通过邮箱和验证码登录并获取 JWT Token")
    @RateLimit(timeWindow = 60, maxRequests = 5, keyPrefix = "login_limit")
    @PostMapping("/login/email")
    public Result<Map<String, Object>> loginByEmail(@Valid @RequestBody UserEmailLoginDTO loginDTO) {
        Map<String, Object> result = userService.loginByEmail(loginDTO);
        return Result.success(result);
    }

    /**
     * 手机验证码登录
     * <p>
     * 登录流程：
     * 1. 验证手机验证码是否正确
     * 2. 根据手机号查询用户
     * 3. 检查账号状态
     * 4. 生成 JWT Token
     * </p>
     * 
     * <p>
     * <b>限流保护</b>：60 秒内最多 5 次尝试
     * </p>
     *
     * @param loginDTO 手机登录请求对象（包含手机号和验证码）
     * @return 包含 Token 和用户信息的 Map
     */
    @Operation(summary = "手机验证码登录", description = "通过手机号和验证码登录并获取 JWT Token")
    @RateLimit(timeWindow = 60, maxRequests = 5, keyPrefix = "login_limit")
    @PostMapping("/login/phone")
    public Result<Map<String, Object>> loginByPhone(@Valid @RequestBody UserPhoneLoginDTO loginDTO) {
        Map<String, Object> result = userService.loginByPhone(loginDTO);
        return Result.success(result);
    }

    /**
     * 根据 ID 查询用户信息
     * <p>
     * 返回数据：
     * <ul>
     *   <li>敏感字段脱敏：邮箱、手机号使用自定义序列化器处理</li>
     *   <li>密码字段隐藏：通过 {@code setPassword(null)} 过滤</li>
     *   <li>原文字段填充：管理员或本人可查看 realEmail、realPhone</li>
     * </ul>
     * </p>
     *
     * @param id 用户 ID
     * @return 用户详细信息
     */
    @Operation(summary = "查询用户", description = "根据用户ID查询用户信息（所有角色可访问）")
    @GetMapping("/{id}")
    public Result<User> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.getUserById(id);
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 从 JWT Token 中提取 userId，查询完整用户信息。
     * 适用于前端页面展示当前用户的头像、昵称、角色等。
     * </p>
     *
     * @return 当前用户的详细信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息（所有角色可访问）")
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        Long currentUserId = permissionService.getCurrentUserId();
        User user = userService.getUserById(currentUserId);
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 查询所有用户列表
     * <p>
     * 返回系统中所有未删除的用户，用于下拉选择、列表展示等场景。
     * 每个用户的密码字段都会被过滤掉。
     * </p>
     *
     * @return 用户列表
     */
    @Operation(summary = "查询所有用户", description = "查询系统中所有用户（所有角色可访问）")
    @GetMapping("/list")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(user -> user.setPassword(null));
        return Result.success(users);
    }

    /**
     * 禁用/启用用户账号
     * <p>
     * 管理员可将违规用户禁用（status=0），被禁用的用户无法登录。
     * 重新启用时设置 status=1。
     * </p>
     * 
     * <p>
     * <b>权限要求</b>：仅管理员（role=2）可操作
     * </p>
     *
     * @param id     用户 ID
     * @param params 包含 status 字段的 Map（0-禁用，1-启用）
     * @return 操作结果
     */
    @Operation(summary = "禁用/启用用户", description = "禁用或启用指定用户（仅管理员）")
    @PutMapping("/{id}/status")
    @RequirePermission(roles = {2})
    public Result<?> updateUserStatus(@Parameter(description = "用户ID") @PathVariable Long id,
                                      @RequestBody Map<String, Integer> params) {
        Integer status = params.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态参数无效，必须为0或1");
        }
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    /**
     * 管理员创建新用户
     * <p>
     * 与用户注册不同，此接口由管理员直接创建账号，无需验证码。
     * 可指定用户角色（普通用户、项目经理、管理员等）。
     * </p>
     * 
     * <p>
     * <b>权限要求</b>：仅管理员（role=2）可操作
     * </p>
     *
     * @param createDTO 创建用户请求对象
     * @return 操作结果
     */
    @Operation(summary = "创建用户", description = "管理员创建新用户（仅管理员）")
    @PostMapping("/create")
    @RequirePermission(roles = {2})
    public Result<?> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        userService.createUser(createDTO);
        return Result.success();
    }

    /**
     * 更新用户信息
     * <p>
     * 支持更新姓名、邮箱、手机号、角色等字段。
     * 非管理员不允许修改角色字段（即使传入也会被忽略）。
     * </p>
     * 
     * <p>
     * <b>权限控制</b>：
     * <ul>
     *   <li>所有人可更新自己的基本信息</li>
     *   <li>仅管理员可修改他人信息和角色</li>
     * </ul>
     * </p>
     *
     * @param id        用户 ID
     * @param updateDTO 更新请求对象
     * @return 操作结果
     */
    @Operation(summary = "更新用户信息", description = "更新用户基本信息（所有角色可访问，但仅管理员可修改角色）")
    @PutMapping("/{id}")
    public Result<?> updateUser(@Parameter(description = "用户ID") @PathVariable Long id,
                                @Valid @RequestBody UserUpdateDTO updateDTO) {
        // 获取当前登录用户
        Long currentUserId = permissionService.getCurrentUserId();
        Integer currentRole = permissionService.getCurrentUserRole();
        
        // 非管理员不允许修改角色
        if (currentRole != 2 && updateDTO.getRole() != null) {
            log.warn("非管理员尝试修改用户角色: userId={}, currentRole={}, targetUserId={}", 
                    currentUserId, currentRole, id);
            return Result.error(403, "权限不足，仅管理员可修改用户角色");
        }
        
        userService.updateUser(id, updateDTO);
        return Result.success();
    }

    /**
     * 更新用户角色
     * <p>
     * 快速修改用户角色的专用接口，例如将普通用户提升为项目经理。
     * </p>
     * 
     * <p>
     * <b>权限要求</b>：仅管理员（role=2）可操作
     * </p>
     *
     * @param id     用户 ID
     * @param params 包含 role 字段的 Map（0-只读，1-普通，2-管理员，3-项目经理）
     * @return 操作结果
     */
    @Operation(summary = "更新用户角色", description = "更新用户角色（仅管理员）")
    @PutMapping("/{id}/role")
    @RequirePermission(roles = {2})
    public Result<?> updateUserRole(@Parameter(description = "用户ID") @PathVariable Long id,
                                    @RequestBody Map<String, Integer> params) {
        Integer role = params.get("role");
        if (role == null || role < 0 || role > 3) {
            return Result.error(400, "角色参数无效，必须为0-3");
        }
        userService.updateUserRole(id, role);
        return Result.success();
    }

    /**
     * 更新当前用户信息
     * <p>
     * 用户可自行修改姓名、邮箱、手机号等基本信息。
     * 不允许通过此接口修改角色和状态（需管理员操作）。
     * </p>
     *
     * @param updateDTO 更新请求对象
     * @return 操作结果
     */
    @Operation(summary = "更新当前用户信息", description = "更新当前登录用户的基本信息")
    @PutMapping("/profile")
    public Result<?> updateProfile(@Valid @RequestBody UserUpdateDTO updateDTO) {
        Long currentUserId = permissionService.getCurrentUserId();
        userService.updateCurrentUser(currentUserId, updateDTO);
        return Result.success();
    }

    /**
     * 修改密码
     * <p>
     * 修改流程：
     * 1. 验证旧密码是否正确
     * 2. 新密码加密后存入数据库
     * 3. 记录操作日志
     * </p>
     * 
     * <p>
     * <b>安全提示</b>：建议前端强制要求新密码长度至少 8 位，包含字母和数字
     * </p>
     *
     * @param passwordDTO 包含旧密码和新密码的请求对象
     * @return 操作结果
     */
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PutMapping("/password")
    public Result<?> updatePassword(@Valid @RequestBody com.demand.module.user.dto.PasswordUpdateDTO passwordDTO) {
        Long currentUserId = permissionService.getCurrentUserId();
        userService.updatePassword(currentUserId, passwordDTO.getOldPassword(), passwordDTO.getNewPassword());
        return Result.success();
    }
}
