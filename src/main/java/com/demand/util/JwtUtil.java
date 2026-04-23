package com.demand.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT（JSON Web Token）工具类
 * <p>
 * 负责 Token 的生成、解析和验证，是系统身份认证的核心组件。
 * 使用 HMAC-SHA256 算法进行签名，确保 Token 的完整性和防篡改性。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>Token 生成</b>：根据用户 ID、用户名、角色生成加密 Token</li>
 *   <li><b>Token 解析</b>：从 Token 中提取用户信息（userId, username, role）</li>
 *   <li><b>Token 验证</b>：检查 Token 是否有效、是否过期、签名是否正确</li>
 * </ul>
 * 
 * <h3>Token 结构：</h3>
 * <pre>
 * Header: {"alg": "HS256", "typ": "JWT"}
 * Payload: {
 *   "userId": 123,
 *   "username": "zhangsan",
 *   "role": 1,
 *   "sub": "zhangsan",
 *   "iat": 1713801600,
 *   "exp": 1713888000
 * }
 * Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
 * </pre>
 * 
 * <h3>配置项（application.properties）：</h3>
 * <ul>
 *   <li>{@code jwt.secret}：签名密钥（建议生产环境使用复杂字符串，至少 32 位）</li>
 *   <li>{@code jwt.expiration}：Token 有效期（毫秒），默认 86400000ms = 24 小时</li>
 * </ul>
 */
@Component
public class JwtUtil {

    /**
     * JWT 签名密钥
     * <p>
     * 用于生成和验证 Token 签名，必须保密。
     * 如果未在配置文件中指定，则使用默认值（仅适用于开发环境）。
     * </p>
     */
    @Value("${jwt.secret:defaultSecretKeyForDemandPlatform2026}")
    private String secret;

    /**
     * Token 有效期（毫秒）
     * <p>
     * 默认 24 小时，可根据业务需求调整：
     * <ul>
     *   <li>短效 Token（1-2 小时）：安全性高，但用户需频繁登录</li>
     *   <li>长效 Token（7-30 天）：用户体验好，但泄露风险增加</li>
     * </ul>
     * </p>
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 获取签名密钥对象
     * <p>
     * 将字符串密钥转换为 HMAC-SHA256 算法所需的 {@link SecretKey} 对象。
     * 每次调用都会创建新实例（JJWT 库要求）。
     * </p>
     *
     * @return HMAC-SHA256 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token（不含角色信息）
     * <p>
     * 适用于简单的身份验证场景，Payload 中仅包含 userId 和 username。
     * </p>
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成 JWT Token（包含角色信息）
     * <p>
     * 适用于需要基于角色进行权限控制的场景，Payload 中包含 userId、username 和 role。
     * </p>
     * 
     * <p>
     * <b>角色编码说明：</b>
     * <ul>
     *   <li>1 - 普通用户</li>
     *   <li>2 - 项目经理</li>
     *   <li>3 - 管理员</li>
     * </ul>
     * </p>
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色编码
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(Long userId, String username, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     * <p>
     * 验证签名并提取 Payload 中的所有声明（Claims）。
     * 如果 Token 无效或已过期，会抛出异常。
     * </p>
     *
     * @param token JWT Token 字符串
     * @return 解析后的 Claims 对象
     * @throws io.jsonwebtoken.JwtException Token 无效或已过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT Token 字符串
     * @return 用户 ID，如果 Token 无效则返回 null
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中提取角色编码
     *
     * @param token JWT Token 字符串
     * @return 角色编码，如果 Token 无效则返回 null
     */
    public Integer getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", Integer.class);
    }

    /**
     * 从 Token 中提取用户名
     *
     * @param token JWT Token 字符串
     * @return 用户名，如果 Token 无效则返回 null
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 Token 是否有效
     * <p>
     * 检查项：
     * <ul>
     *   <li>签名是否正确（防止篡改）</li>
     *   <li>是否已过期（exp 字段）</li>
     *   <li>格式是否符合 JWT 规范</li>
     * </ul>
     * </p>
     *
     * @param token JWT Token 字符串
     * @return true 表示 Token 有效，false 表示无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
