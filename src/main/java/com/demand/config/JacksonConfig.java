package com.demand.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化/反序列化配置类
 * <p>
 * 主要用于解决 Java 8 时间类型（如 LocalDateTime）在 JSON 转换时的兼容性问题。
 * 默认情况下，Jackson 可能会将时间序列化为时间戳或复杂的对象结构，
 * 本配置统一将其格式化为 "yyyy-MM-dd HH:mm:ss" 字符串，方便前后端交互。
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 全局日期时间格式模式
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 配置全局 ObjectMapper 实例
     * <p>
     * 核心配置项：
     * 1. JavaTimeModule：支持 Java 8 新增的时间类型（LocalDateTime, LocalDate 等）
     * 2. 自定义序列化器/反序列化器：统一使用 yyyy-MM-dd HH:mm:ss 格式
     * 3. 禁用时间戳：禁止将日期转换为数字时间戳，始终返回字符串
     * </p>
     *
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 创建 Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 2. 定义日期格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        
        // 3. 注册 LocalDateTime 的序列化与反序列化规则
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        // 4. 将模块注册到 ObjectMapper
        objectMapper.registerModule(javaTimeModule);

        // 5. 禁用时间戳特性（确保输出的是字符串而不是数字）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
