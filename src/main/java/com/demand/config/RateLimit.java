package com.demand.config;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * <p>
 * 用于标记需要进行访问频率限制的接口。
 * 通过 AOP 拦截器结合 Redis 计数器实现基于 IP 或用户的限流。
 * </p>
 * <p>
 * 使用场景：
 * 1. 登录/注册接口：防止恶意暴力破解或短信轰炸
 * 2. 导出/报表接口：防止高频查询拖垮数据库
 * 3. 文件上传接口：防止恶意占用带宽和存储空间
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * @RateLimit(timeWindow = 60, maxRequests = 5, keyPrefix = "login")
 * public Result login(@RequestBody LoginDTO dto) { ... }
 * </pre>
 * 上述示例表示：60 秒内最多允许 5 次请求，超出则抛出 429 异常。
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流时间窗口（单位：秒）
     * <p>
     * 默认 60 秒。表示在该时间段内统计请求次数。
     * </p>
     *
     * @return 时间窗口长度
     */
    int timeWindow() default 60;

    /**
     * 最大允许请求次数
     * <p>
     * 默认 10 次。在 timeWindow 指定的时间内，超过此次数将被拦截。
     * </p>
     *
     * @return 请求次数上限
     */
    int maxRequests() default 10;

    /**
     * 限流 Key 的前缀
     * <p>
     * 默认 "rate_limit"。实际存入 Redis 的 Key 格式为：
     * {keyPrefix}:{URI}:{IP}
     * 自定义前缀便于在 Redis 中分类管理和清理数据。
     * </p>
     *
     * @return Key 前缀字符串
     */
    String keyPrefix() default "rate_limit";
}
