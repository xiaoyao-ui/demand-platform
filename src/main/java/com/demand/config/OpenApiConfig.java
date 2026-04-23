package com.demand.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger）在线文档配置类
 * <p>
 * 集成 SpringDoc OpenAPI，自动生成 RESTful API 接口文档。
 * 开发者访问 /swagger-ui.html 即可查看、测试所有接口。
 * </p>
 * <p>
 * 核心功能：
 * 1. 自动扫描所有 Controller 和 @Operation 注解，生成接口列表
 * 2. 支持在线调试：可直接在页面输入参数并发送请求
 * 3. 导出文档：支持导出为 JSON/YAML 格式，供第三方工具使用
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 自定义 OpenAPI 文档元信息
     * <p>
     * 配置内容包括：
     * 1. title：文档标题，显示在 Swagger UI 顶部
     * 2. description：项目描述，帮助使用者了解平台功能
     * 3. version：API 版本号，便于区分不同迭代
     * 4. contact：作者联系方式，方便使用者反馈问题
     * 5. license：开源协议声明
     * </p>
     *
     * @return 配置好的 OpenAPI 实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        // API 文档标题
                        .title("需求平台 API 文档")
                        // 平台功能简介
                        .description("需求管理平台的 RESTful API 接口文档")
                        // 当前版本号
                        .version("v1.0.0")
                        // 作者联系信息
                        .contact(new Contact()
                                .name("李逍遥")
                                .email("lixiaoyao@example.com"))
                        // 开源许可证
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
