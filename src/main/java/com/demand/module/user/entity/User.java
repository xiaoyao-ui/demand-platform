package com.demand.module.user.entity;

import com.demand.util.EmailSerializer;
import com.demand.util.PhoneSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 对应数据库 {@code user} 表，存储用户的基本信息和认证凭据。
 * 集成了 Jackson 序列化注解，实现敏感字段的自动脱敏。
 * </p>
 * 
 * <h3>字段说明：</h3>
 * <ul>
 *   <li><b>password</b>：使用 {@code @JsonIgnore} 完全隐藏，不会出现在 JSON 响应中</li>
 *   <li><b>email/phone</b>：使用自定义序列化器脱敏显示（如 138****5678）</li>
 *   <li><b>realEmail/realPhone</b>：原文字段，仅管理员或本人可见</li>
 * </ul>
 * 
 * <h3>脱敏逻辑：</h3>
 * <p>
 * 在 {@link UserService#fillSensitiveInfo} 中根据当前用户角色填充原文字段：
 * <ul>
 *   <li>管理员（role=2）：查看所有用户原文</li>
 *   <li>本人：查看自己的原文</li>
 *   <li>其他人：原文字段为 null，前端仅显示脱敏后的 email/phone</li>
 * </ul>
 * </p>
 */
@Data
public class User {
    /**
     * 用户 ID（主键）
     */
    private Long id;

    /**
     * 用户名（登录账号）
     */
    private String username;
    
    /**
     * 密码（BCrypt 加密存储）
     * <p>
     * 使用 {@code @JsonIgnore} 确保永远不会序列化到 JSON 响应中
     * </p>
     */
    @JsonIgnore
    private String password;
    
    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 头像 URL（如 /avatar/2026/04/23/xxx.jpg）
     */
    private String avatar;
    
    /**
     * 邮箱地址（脱敏显示）
     * <p>
     * 使用 {@link EmailSerializer} 自动脱敏，例如：zh***@example.com
     * </p>
     */
    @JsonSerialize(using = EmailSerializer.class)
    private String email;

    /**
     * 手机号（脱敏显示）
     * <p>
     * 使用 {@link PhoneSerializer} 自动脱敏，例如：138****5678
     * </p>
     */
    @JsonSerialize(using = PhoneSerializer.class)
    private String phone;
    
    /**
     * 邮箱原文（仅管理员或本人可见）
     * <p>
     * 由 {@link UserService#fillSensitiveInfo} 根据权限填充
     * </p>
     */
    private String realEmail;

    /**
     * 手机号原文（仅管理员或本人可见）
     * <p>
     * 由 {@link UserService#fillSensitiveInfo} 根据权限填充
     * </p>
     */
    private String realPhone;
    
    /**
     * 角色编码
     * <ul>
     *   <li>0 - 只读用户</li>
     *   <li>1 - 普通用户</li>
     *   <li>2 - 管理员</li>
     *   <li>3 - 项目经理</li>
     * </ul>
     */
    private Integer role;

    /**
     * 账号状态
     * <ul>
     *   <li>0 - 禁用</li>
     *   <li>1 - 启用</li>
     * </ul>
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
