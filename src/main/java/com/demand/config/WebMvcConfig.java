package com.demand.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局配置类
 * <p>
 * 负责配置拦截器链和静态资源映射，是请求处理的核心入口。
 * 主要功能包括：
 * 1. 注册 JWT 身份验证拦截器和限流拦截器
 * 2. 配置文件上传目录的静态资源访问路径
 * 3. 排除不需要认证的公开接口（登录、注册、Swagger 等）
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * JWT 身份验证拦截器
     * <p>
     * 从请求头中提取 Token，验证用户身份并将用户信息存入 {@link PermissionContext}
     * </p>
     */
    private final JwtInterceptor jwtInterceptor;

    /**
     * 接口限流拦截器
     * <p>
     * 基于 Redis 计数器实现接口级别的访问频率控制，防止恶意刷接口
     * </p>
     */
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 文件上传根路径（从 application.properties 读取）
     * <p>
     * 例如：D:/uploads 或 /app/uploads
     * </p>
     */
    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 注册拦截器链
     * <p>
     * 拦截顺序：JWT 拦截器 -> 限流拦截器 -> Controller
     * </p>
     * 
     * <h3>JWT 拦截器配置：</h3>
     * <ul>
     *   <li><b>拦截路径</b>：{@code /api/**}（所有 API 接口）</li>
     *   <li><b>排除路径</b>：
     *     <ul>
     *       <li>登录/注册接口：{@code /api/user/login}, {@code /api/user/register}</li>
     *       <li>验证码接口：{@code /api/verification/**}</li>
     *       <li>公开下载接口：附件下载、Excel 导出</li>
     *       <li>头像访问：{@code /avatar/**}</li>
     *       <li>API 文档：Swagger UI、OpenAPI JSON</li>
     *       <li>健康检查：{@code /actuator/**}</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <h3>限流拦截器配置：</h3>
     * <ul>
     *   <li><b>拦截路径</b>：{@code /api/**}（对所有 API 生效）</li>
     *   <li><b>限流规则</b>：通过 {@link RateLimit} 注解定义具体接口的限流策略</li>
     * </ul>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 注册 JWT 身份验证拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/login/email",
                        "/api/user/login/phone",
                        "/api/user/register",
                        "/api/verification/**",
                        "/api/attachment/download/**",
                        "/api/log/export/excel",
                        "/api/demand/export/excel",
                        "/avatar/**",
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**"
                );

        // 2. 注册限流拦截器（对所有 API 生效）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * 配置静态资源处理器
     * <p>
     * 将虚拟路径映射到物理文件系统，支持头像、附件等文件的 HTTP 访问。
     * </p>
     * 
     * <h3>映射规则：</h3>
     * <ul>
     *   <li><b>{@code /avatar/**}</b> → {@code ${file.upload.path}/avatar/}<br>
     *       例如：{@code http://localhost:8080/avatar/123.jpg} 映射到 {@code D:/uploads/avatar/123.jpg}</li>
     *   
     *   <li><b>{@code /api/attachment/**}</b> → {@code ${file.upload.path}/}<br>
     *       例如：{@code http://localhost:8080/api/attachment/2026/04/20/file.pdf} 映射到 {@code D:/uploads/2026/04/20/file.pdf}</li>
     *   
     *   <li><b>{@code /ws/**}</b> → 禁用静态资源处理<br>
     *       防止 Spring 将 WebSocket 路径当作静态资源拦截，确保 {@link NotificationWebSocketHandler} 正常工作</li>
     * </ul>
     * 
     * <p>
     * <b>路径转换逻辑：</b><br>
     * 如果配置的路径是相对路径（如 {@code ./uploads}），会自动转换为绝对路径，
     * 确保在不同操作系统（Windows/Linux）下都能正确访问。
     * </p>
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 将相对路径转换为绝对路径（兼容 Windows 和 Linux）
        String absolutePath = uploadPath;
        if (absolutePath.startsWith("./") || absolutePath.startsWith(".\\")) {
            absolutePath = new java.io.File(absolutePath).getAbsolutePath();
        }
        
        // 2. 配置头像访问路径
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + absolutePath + "/avatar/");
        
        // 3. 配置附件访问路径
        registry.addResourceHandler("/api/attachment/**")
                .addResourceLocations("file:" + absolutePath + "/");

        // 4. 阻止 Spring 将 /ws/** 当作静态资源处理（让 WebSocket 接管）
        registry.addResourceHandler("/ws/**")
                .addResourceLocations("classpath:/")
                .resourceChain(false);
    }
}
