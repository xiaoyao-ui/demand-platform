package com.demand.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置类（生产环境）
 * <p>
 * 负责配置 Spring Security 的核心行为，包括会话管理、CSRF 防护、请求授权等。
 * 通过 {@link Profile("!test")} 注解确保仅在非测试环境下生效。
 * </p>
 * 
 * <h3>核心设计思路：</h3>
 * <ul>
 *   <li><b>无状态认证</b>：使用 JWT + 自定义拦截器实现身份验证，不依赖 Session</li>
 *   <li><b>放行所有请求</b>：由 {@link JwtInterceptor} 和 {@link PermissionInterceptor} 处理鉴权逻辑</li>
 *   <li><b>禁用 CSRF</b>：前后端分离架构中，JWT 本身已具备防重放攻击能力</li>
 * </ul>
 */
@Configuration
@Profile("!test")
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤链
     * <p>
     * 主要配置项：
     * 1. 禁用 CSRF 保护（前后端分离项目无需 Cookie 认证）
     * 2. 设置会话策略为 STATELESS（无状态，不使用 Session）
     * 3. 放行所有 HTTP 请求（权限校验交给自定义拦截器处理）
     * </p>
     * 
     * <p>
     * <b>为什么放行所有请求？</b><br>
     * 本项目采用 JWT + 自定义拦截器（{@link JwtInterceptor}）进行身份验证，
     * 而非传统的 Spring Security Filter Chain。这样可以更灵活地控制鉴权逻辑，
     * 并支持基于角色的细粒度权限校验（{@link RequirePermission}）。
     * </p>
     *
     * @param http HttpSecurity 配置对象
     * @return 构建好的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * 配置认证管理器
     * <p>
     * 提供 AuthenticationManager Bean，用于用户身份验证。
     * 虽然本项目使用 JWT 拦截器进行鉴权，但保留此配置以兼容 Spring Security 的其他功能。
     * </p>
     *
     * @param config Spring Security 自动配置对象
     * @return 认证管理器实例
     * @throws Exception 获取认证管理器失败
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置用户详情服务
     * <p>
     * 返回一个空的 UserDetailsService 实现。
     * 由于本项目使用自定义的 JWT 拦截器和数据库查询进行用户验证，
     * 因此不需要 Spring Security 默认的 UserDetails 机制。
     * </p>
     *
     * @return 空的 UserDetailsService 实现
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> null;
    }
}
