package com.demand.module.user.service;

import com.demand.exception.BusinessException;
import com.demand.module.user.dto.UserEmailLoginDTO;
import com.demand.module.user.dto.UserLoginDTO;
import com.demand.module.user.dto.UserPhoneLoginDTO;
import com.demand.module.user.dto.UserRegisterDTO;
import com.demand.module.user.dto.UserCreateDTO;
import com.demand.module.user.dto.UserUpdateDTO;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import com.demand.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户业务逻辑服务
 * <p>
 * 负责处理用户注册、登录、信息管理、权限控制等核心业务逻辑。
 * 集成 BCrypt 密码加密、JWT Token 生成、验证码验证等功能。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>用户认证</b>：三种登录方式（用户名密码、邮箱验证码、手机验证码）</li>
 *   <li><b>用户管理</b>：创建、更新、禁用/启用、修改密码</li>
 *   <li><b>敏感信息保护</b>：邮箱/手机号脱敏展示，仅管理员或本人可见原文</li>
 *   <li><b>安全防护</b>：密码加密存储、验证码校验、账号状态检查</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * JWT 工具类，用于生成和解析 Token
     */
    private final JwtUtil jwtUtil;

    /**
     * 验证码服务，用于发送和验证邮箱/手机验证码
     */
    private final VerificationCodeService verificationCodeService;

    /**
     * 密码编码器（BCrypt 算法）
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 权限验证服务
     */
    private final PermissionService permissionService;

    /**
     * 用户注册
     * <p>
     * 注册流程：
     * 1. 校验用户名是否已存在
     * 2. 验证邮箱格式和验证码正确性
     * 3. 密码使用 BCrypt 加密后存入数据库
     * 4. 默认分配"普通用户"角色（role=1），状态为启用（status=1）
     * </p>
     *
     * @param registerDTO 注册请求对象（包含用户名、密码、邮箱、验证码等）
     * @throws BusinessException 当用户名已存在或验证码错误时抛出
     */
    public void register(UserRegisterDTO registerDTO) {
        log.info("用户注册请求: username={}", registerDTO.getUsername());

        // 1. 检查用户名是否已存在
        User existUser = userMapper.findByUsername(registerDTO.getUsername());
        if (existUser != null) {
            log.warn("用户注册失败，用户名已存在: {}", registerDTO.getUsername());
            throw new BusinessException("用户名已存在");
        }

        // 2. 验证至少提供一个验证码（邮箱或手机）
        boolean hasEmailCode = registerDTO.getEmailCode() != null && !registerDTO.getEmailCode().isEmpty();
        boolean hasPhoneCode = registerDTO.getPhoneCode() != null && !registerDTO.getPhoneCode().isEmpty();
        
        if (!hasEmailCode && !hasPhoneCode) {
            throw new BusinessException("请至少提供邮箱验证码或手机验证码");
        }

        // 3. 验证邮箱格式（如果提供了邮箱）
        if (registerDTO.getEmail() != null && !registerDTO.getEmail().isEmpty()) {
            if (!registerDTO.getEmail().matches("^[\\w-\\.]+@[\\w-\\.]+\\.[a-z]{2,}$")) {
                throw new BusinessException("邮箱格式不正确");
            }
        }

        // 4. 验证手机号格式（如果提供了手机号）
        if (registerDTO.getPhone() != null && !registerDTO.getPhone().isEmpty()) {
            if (!registerDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException("手机号格式不正确");
            }
        }

        // 5. 验证验证码（优先验证邮箱，其次验证手机）
        boolean codeValid = false;
        if (hasEmailCode) {
            if (registerDTO.getEmail() == null || registerDTO.getEmail().isEmpty()) {
                throw new BusinessException("邮箱不能为空");
            }
            codeValid = verificationCodeService.verifyCode(registerDTO.getEmail(), registerDTO.getEmailCode(), "email");
            if (!codeValid) {
                throw new BusinessException("邮箱验证码错误或已过期");
            }
        } else if (hasPhoneCode) {
            if (registerDTO.getPhone() == null || registerDTO.getPhone().isEmpty()) {
                throw new BusinessException("手机号不能为空");
            }
            codeValid = verificationCodeService.verifyCode(registerDTO.getPhone(), registerDTO.getPhoneCode(), "phone");
            if (!codeValid) {
                throw new BusinessException("手机验证码错误或已过期");
            }
        }

        // 6. 创建用户对象并设置默认值
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setRealName(registerDTO.getRealName());
        user.setRole(1);  // 默认普通用户
        user.setStatus(1); // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * 用户名密码登录
     * <p>
     * 登录流程：
     * 1. 根据用户名查询用户
     * 2. 验证密码是否正确（BCrypt 比对）
     * 3. 检查账号状态（是否被禁用）
     * 4. 生成 JWT Token（包含 userId、username、role）
     * 5. 清除密码字段后返回用户信息
     * </p>
     *
     * @param loginDTO 登录请求对象（包含用户名和密码）
     * @return 包含 Token 和用户信息的 Map
     * @throws BusinessException 当用户名/密码错误或账号被禁用时抛出
     */
    public Map<String, Object> login(UserLoginDTO loginDTO) {
        log.info("用户登录请求: username={}", loginDTO.getUsername());

        // 1. 查询用户
        User user = userMapper.findByUsername(loginDTO.getUsername());
        if (user == null) {
            log.warn("用户登录失败，用户名不存在: {}", loginDTO.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            log.warn("用户登录失败，密码错误: username={}", loginDTO.getUsername());
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查账号状态
        if (user.getStatus() == 0) {
            log.warn("用户登录失败，账号已被禁用: userId={}", user.getId());
            throw new BusinessException("账号已被禁用");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        user.setPassword(null); // 清除密码字段
        result.put("user", user);

        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return result;
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
     * @param loginDTO 邮箱登录请求对象（包含邮箱和验证码）
     * @return 包含 Token 和用户信息的 Map
     * @throws BusinessException 当验证码错误或邮箱未注册时抛出
     */
    public Map<String, Object> loginByEmail(UserEmailLoginDTO loginDTO) {
        log.info("邮箱验证码登录请求: email={}", loginDTO.getEmail());

        // 1. 验证邮箱验证码
        if (!verificationCodeService.verifyCode(loginDTO.getEmail(), loginDTO.getEmailCode(), "email")) {
            throw new BusinessException("邮箱验证码错误或已过期");
        }

        // 2. 查询用户
        User user = userMapper.findByEmail(loginDTO.getEmail());
        if (user == null) {
            log.warn("邮箱登录失败，邮箱未注册: {}", loginDTO.getEmail());
            throw new BusinessException("该邮箱未注册");
        }

        // 3. 检查账号状态
        if (user.getStatus() == 0) {
            log.warn("邮箱登录失败，账号已被禁用: userId={}", user.getId());
            throw new BusinessException("账号已被禁用");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        user.setPassword(null);
        result.put("user", user);

        log.info("邮箱登录成功: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());
        return result;
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
     * @param loginDTO 手机登录请求对象（包含手机号和验证码）
     * @return 包含 Token 和用户信息的 Map
     * @throws BusinessException 当验证码错误或手机号未注册时抛出
     */
    public Map<String, Object> loginByPhone(UserPhoneLoginDTO loginDTO) {
        log.info("手机验证码登录请求: phone={}", loginDTO.getPhone());

        // 1. 验证手机验证码
        if (!verificationCodeService.verifyCode(loginDTO.getPhone(), loginDTO.getPhoneCode(), "phone")) {
            throw new BusinessException("手机验证码错误或已过期");
        }

        // 2. 查询用户
        User user = userMapper.findByPhone(loginDTO.getPhone());
        if (user == null) {
            log.warn("手机登录失败，手机号未注册: {}", loginDTO.getPhone());
            throw new BusinessException("该手机号未注册");
        }

        // 3. 检查账号状态
        if (user.getStatus() == 0) {
            log.warn("手机登录失败，账号已被禁用: userId={}", user.getId());
            throw new BusinessException("账号已被禁用");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        user.setPassword(null);
        result.put("user", user);

        log.info("手机登录成功: userId={}, phone={}, role={}", user.getId(), user.getPhone(), user.getRole());
        return result;
    }

    /**
     * 根据 ID 查询用户信息
     * <p>
     * 返回数据包含：
     * <ul>
     *   <li>脱敏字段：email、phone（使用自定义序列化器处理）</li>
     *   <li>原文字段：realEmail、realPhone（仅管理员或本人可见）</li>
     *   <li>密码字段：始终返回 null</li>
     * </ul>
     * </p>
     *
     * @param id 用户 ID
     * @return 用户详细信息
     * @throws BusinessException 当用户不存在时抛出
     */
    public User getUserById(Long id) {
        log.debug("查询用户信息: userId={}", id);
        User user = userMapper.findById(id);
        if (user == null) {
            log.warn("用户不存在: userId={}", id);
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        
        // 获取当前用户信息，用于判断是否有权限查看原文
        Long currentUserId = permissionService.getCurrentUserId();
        Integer currentRole = permissionService.getCurrentUserRole();
        fillSensitiveInfo(user, currentUserId, currentRole); // 填充原文字段
        
        return user;
    }

    /**
     * 查询所有用户列表
     * <p>
     * 返回系统中所有未删除的用户，每个用户的密码字段都会被过滤掉。
     * 敏感信息（邮箱、手机）根据当前用户角色进行脱敏或显示原文。
     * </p>
     *
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        log.debug("查询所有用户");
        List<User> users = userMapper.selectAllUsers();
        Long currentUserId = permissionService.getCurrentUserId();
        Integer currentRole = permissionService.getCurrentUserRole();

        users.forEach(u -> {
            u.setPassword(null);
            fillSensitiveInfo(u, currentUserId, currentRole); // 填充原文字段
        });
        return users;
    }

    /**
     * 更新用户状态（禁用/启用）
     * <p>
     * 被禁用的用户（status=0）无法登录系统。
     * 管理员可通过此接口恢复被禁用用户的访问权限。
     * </p>
     *
     * @param id     用户 ID
     * @param status 新状态（0-禁用，1-启用）
     * @throws BusinessException 当用户不存在时抛出
     */
    public void updateUserStatus(Long id, Integer status) {
        log.info("更新用户状态: userId={}, status={}", id, status);
        
        User user = userMapper.findById(id);
        if (user == null) {
            log.warn("用户不存在，状态更新失败: userId={}", id);
            throw new BusinessException("用户不存在");
        }
        
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateStatus(id, status);
        
        log.info("用户状态更新成功: userId={}, status={}", id, status);
    }

    /**
     * 管理员创建用户
     * <p>
     * 与用户注册不同，此方法由管理员直接创建账号，无需验证码。
     * 可指定用户角色（普通用户、项目经理、管理员等）。
     * </p>
     *
     * @param createDTO 创建用户请求对象
     * @throws BusinessException 当用户名或邮箱已存在时抛出
     */
    public void createUser(UserCreateDTO createDTO) {
        log.info("管理员创建用户: username={}, role={}", createDTO.getUsername(), createDTO.getRole());

        // 1. 检查用户名是否已存在
        User existUser = userMapper.findByUsername(createDTO.getUsername());
        if (existUser != null) {
            log.warn("创建用户失败，用户名已存在: {}", createDTO.getUsername());
            throw new BusinessException("用户名已存在");
        }

        // 2. 检查邮箱是否已存在
        if (createDTO.getEmail() != null && !createDTO.getEmail().isEmpty()) {
            User existEmail = userMapper.findByEmail(createDTO.getEmail());
            if (existEmail != null) {
                log.warn("创建用户失败，邮箱已存在: {}", createDTO.getEmail());
                throw new BusinessException("邮箱已存在");
            }
        }

        // 3. 创建用户对象
        User user = new User();
        user.setUsername(createDTO.getUsername());
        user.setPassword(passwordEncoder.encode(createDTO.getPassword()));
        user.setRealName(createDTO.getRealName());
        user.setEmail(createDTO.getEmail());
        user.setPhone(createDTO.getPhone());
        user.setRole(createDTO.getRole());
        user.setStatus(1); // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("用户创建成功: userId={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * 更新用户信息
     * <p>
     * 支持更新姓名、邮箱、手机号、角色等字段。
     * 会检查邮箱是否被其他用户使用，防止冲突。
     * </p>
     *
     * @param id        用户 ID
     * @param updateDTO 更新请求对象
     * @throws BusinessException 当用户不存在或邮箱已被使用时抛出
     */
    public void updateUser(Long id, UserUpdateDTO updateDTO) {
        log.info("更新用户信息: userId={}", id);

        User user = userMapper.findById(id);
        if (user == null) {
            log.warn("用户不存在，更新失败: userId={}", id);
            throw new BusinessException("用户不存在");
        }

        // 检查邮箱是否被其他用户使用
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty()) {
            User existEmail = userMapper.findByEmail(updateDTO.getEmail());
            if (existEmail != null && !existEmail.getId().equals(id)) {
                log.warn("更新用户失败，邮箱已被其他用户使用: {}", updateDTO.getEmail());
                throw new BusinessException("邮箱已被其他用户使用");
            }
        }

        // 更新字段
        if (updateDTO.getRealName() != null) {
            user.setRealName(updateDTO.getRealName());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getRole() != null) {
            user.setRole(updateDTO.getRole());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);

        log.info("用户信息更新成功: userId={}", id);
    }

    /**
     * 更新用户角色
     * <p>
     * 快速修改用户角色的专用方法，例如将普通用户提升为项目经理。
     * </p>
     *
     * @param id   用户 ID
     * @param role 新角色编码（0-只读，1-普通，2-管理员，3-项目经理）
     * @throws BusinessException 当用户不存在时抛出
     */
    public void updateUserRole(Long id, Integer role) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateRole(id, role);
        
        log.info("用户角色更新成功: userId={}, newRole={}", id, role);
    }

    /**
     * 更新当前用户信息
     * <p>
     * 用户可自行修改姓名、邮箱、手机号等基本信息。
     * 不允许通过此方法修改角色和状态（需管理员操作）。
     * </p>
     *
     * @param userId    当前用户 ID
     * @param updateDTO 更新请求对象
     * @throws BusinessException 当用户不存在或邮箱已被使用时抛出
     */
    public void updateCurrentUser(Long userId, UserUpdateDTO updateDTO) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 只允许更新基本信息，不允许更新角色和状态
        if (updateDTO.getRealName() != null) {
            user.setRealName(updateDTO.getRealName());
        }
        
        if (updateDTO.getEmail() != null) {
            // 判断邮箱是否修改
            boolean emailChanged = !updateDTO.getEmail().equals(user.getEmail());
            
            if (emailChanged) {
                // 邮箱修改了，需要验证验证码
                if (updateDTO.getEmailCode() == null || updateDTO.getEmailCode().isEmpty()) {
                    throw new BusinessException("修改邮箱需要验证码");
                }
                
                // 验证邮箱验证码
                if (!verificationCodeService.verifyCode(updateDTO.getEmail(), updateDTO.getEmailCode(), "email")) {
                    throw new BusinessException("邮箱验证码错误或已过期");
                }
                
                // 检查邮箱是否被其他用户使用
                User existUser = userMapper.findByEmail(updateDTO.getEmail());
                if (existUser != null && !existUser.getId().equals(userId)) {
                    throw new BusinessException("邮箱已被其他用户使用");
                }
            }
            
            user.setEmail(updateDTO.getEmail());
        }
        
        if (updateDTO.getPhone() != null) {
            // 判断手机号是否修改
            boolean phoneChanged = user.getPhone() == null || !updateDTO.getPhone().equals(user.getPhone());
            
            if (phoneChanged) {
                // 手机号修改了，需要验证验证码
                if (updateDTO.getPhoneCode() == null || updateDTO.getPhoneCode().isEmpty()) {
                    throw new BusinessException("修改手机号需要验证码");
                }
                
                // 验证手机号格式
                if (!updateDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
                    throw new BusinessException("手机号格式不正确");
                }
                
                // 验证手机验证码
                if (!verificationCodeService.verifyCode(updateDTO.getPhone(), updateDTO.getPhoneCode(), "phone")) {
                    throw new BusinessException("手机验证码错误或已过期");
                }
            }
            
            user.setPhone(updateDTO.getPhone());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
        
        log.info("用户信息更新成功: userId={}", userId);
    }

    /**
     * 修改密码
     * <p>
     * 修改流程：
     * 1. 验证旧密码是否正确
     * 2. 新密码使用 BCrypt 加密后存入数据库
     * 3. 记录操作日志
     * </p>
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @throws BusinessException 当用户不存在或旧密码错误时抛出
     */
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("当前密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
        
        log.info("用户密码修改成功: userId={}", userId);
    }

    /**
     * 更新用户头像
     * <p>
     * 将头像 URL 存入数据库，实际文件由 {@link AvatarController} 保存到磁盘。
     * </p>
     *
     * @param userId    用户 ID
     * @param avatarUrl 头像访问路径（如 /avatar/2026/04/23/xxx.jpg）
     * @throws BusinessException 当用户不存在时抛出
     */
    public void updateUserAvatar(Long userId, String avatarUrl) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        user.setAvatar(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
        
        log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
    }

    /**
     * 填充敏感信息原文
     * <p>
     * 根据当前用户角色和所有权关系，决定是否返回邮箱和手机号的原文：
     * <ul>
     *   <li>管理员（role=2）：查看所有用户原文</li>
     *   <li>本人：查看自己的原文</li>
     *   <li>其他人：仅返回脱敏后的字段（realEmail/realPhone 为 null）</li>
     * </ul>
     * </p>
     *
     * @param user          用户对象
     * @param currentUserId 当前登录用户 ID
     * @param currentRole   当前登录用户角色
     */
    private void fillSensitiveInfo(User user, Long currentUserId, Integer currentRole) {
        if (user == null) return;

        // 管理员(role=2) 或 查看本人信息，才返回原文
        boolean hasPermission = currentRole == 2 || user.getId().equals(currentUserId);

        if (hasPermission) {
            user.setRealPhone(user.getPhone());
            user.setRealEmail(user.getEmail());
        } else {
            user.setRealPhone(null);
            user.setRealEmail(null);
        }
    }
}
