package com.demand.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 全局配置类
 * <p>
 * 用于配置 MyBatis 框架的扫描路径和相关行为。
 * 通过 @MapperScan 注解自动扫描并注册所有的 Mapper 接口，
 * 避免在每个 Mapper 接口上重复添加 @Mapper 注解。
 * </p>
 * <p>
 * 扫描规则说明：
 * 1. "com.demand.module.*.mapper"：使用通配符匹配所有模块下的 mapper 包
 * 2. 例如：com.demand.module.user.mapper、com.demand.module.demand.mapper 等
 * 3. 新增业务模块时，只要符合该目录结构，无需修改此配置即可自动识别
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Configuration
@MapperScan("com.demand.module.*.mapper")
public class MybatisConfig {
}