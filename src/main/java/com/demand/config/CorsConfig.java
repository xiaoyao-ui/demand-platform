package com.demand.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域资源共享（CORS）全局配置类
 * <p>
 * 由于项目采用前后端分离架构（前端运行在 localhost:3000，后端运行在 localhost:8080），
 * 浏览器出于安全考虑会拦截跨域请求。本配置类用于解除该限制，允许前端正常访问后端接口。
 * </p>
 * <p>
 * 安全说明：
 * 1. 当前配置允许所有来源（*）访问，仅适用于开发/演示环境。
 * 2. 若部署到生产环境且有固定域名，建议将 allowedOriginPattern 改为具体的域名。
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * 注册全局 CORS 过滤器
     * <p>
     * 配置规则：
     * 1. allowedOriginPattern("*")：允许所有域名发起请求
     * 2. allowedHeader("*")：允许所有请求头（如 Authorization, Content-Type）
     * 3. allowedMethod("*")：允许所有 HTTP 方法（GET, POST, PUT, DELETE 等）
     * 4. allowCredentials(true)：允许携带 Cookie/Token 等认证信息
     * 5. maxAge(3600L)：预检请求（OPTIONS）的缓存时间为 1 小时，减少浏览器请求次数
     * </p>
     *
     * @return CorsFilter 跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许所有域名跨域访问
        config.addAllowedOriginPattern("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");
        // 允许携带认证信息（Cookie、Authorization header）
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒），1小时内相同跨域请求不再发送 OPTIONS 预检
        config.setMaxAge(3600L);

        // 将配置应用到所有以 /api/ 开头的接口路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        log.info("CORS 配置已启用：允许所有来源访问 /api/** 路径");
        return new CorsFilter(source);
    }
}
