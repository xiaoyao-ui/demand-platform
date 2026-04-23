package com.demand.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 全局配置类
 * <p>
 * 负责配置 RedisTemplate 和 CacheManager，确保数据在 Redis 中的存储格式符合预期。
 * 核心解决了 LocalDateTime 等 Java 8 时间类型的序列化问题，并开启了多态类型支持，
 * 防止从缓存读取对象时出现 ClassCastException。
 * </p>
 */
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 注入 Spring 容器中的 ObjectMapper 实例
     * <p>
     * 该 ObjectMapper 已在 JacksonConfig 中配置了时间格式化规则，
     * 保证 Redis 缓存与 HTTP 响应使用统一的序列化策略。
     * </p>
     */
    private final ObjectMapper objectMapper;

    public RedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 配置 RedisTemplate，用于直接操作 Redis
     * <p>
     * 主要设置：
     * 1. Key 和 HashKey 使用 StringRedisSerializer（人类可读）
     * 2. Value 默认不设置序列化器（由调用方自行决定）
     * </p>
     *
     * @param connectionFactory Redis 连接工厂，由 Spring Boot 自动配置
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 Spring Cache 的缓存管理器
     * <p>
     * 核心配置项：
     * 1. JavaTimeModule：支持 LocalDateTime 等 Java 8 时间类型，避免序列化为时间戳
     * 2. activateDefaultTyping：开启多态类型支持，将类名作为 @class 属性存入 JSON，
     *    防止反序列化时因类型擦除导致 ClassCastException
     * 3. entryTtl：设置缓存默认过期时间为 10 分钟，防止缓存无限增长
     * </p>
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. 创建 ObjectMapper（独立实例，不影响 HTTP 响应序列化）
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. 注册 Java 8 时间模块，让 LocalDateTime 以 "yyyy-MM-dd HH:mm:ss" 格式存储
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. 开启多态类型处理，把类名作为属性（@class）存入 JSON
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // 4. 创建支持类型信息的 JSON 序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 5. 配置缓存管理器
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
