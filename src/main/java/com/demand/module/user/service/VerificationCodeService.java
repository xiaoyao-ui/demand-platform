package com.demand.module.user.service;

import com.demand.exception.BusinessException;
import com.demand.module.notification.dto.VerificationCodeNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 * <p>
 * 负责生成、发送、验证手机和邮箱验证码。
 * 集成 Redis 实现多重限流保护，防止短信/邮件轰炸攻击。
 * </p>
 * 
 * <h3>限流策略：</h3>
 * <ul>
 *   <li><b>单目标限流</b>：同一手机号/邮箱 60 秒内只能发送 1 次</li>
 *   <li><b>每日上限</b>：同一手机号/邮箱每天最多发送 5 次</li>
 *   <li><b>IP 限流</b>：同一 IP 地址 60 秒内最多发送 3 次</li>
 *   <li><b>验证失败限制</b>：连续错误 5 次后锁定 10 分钟</li>
 * </ul>
 * 
 * <h3>Redis Key 设计：</h3>
 * <pre>
 * verification:phone:{phone}          → 验证码（TTL=300s）
 * verification:email:{email}          → 验证码（TTL=300s）
 * verification:limit:phone:{phone}    → 发送频率限制（TTL=60s）
 * verification:daily:phone:{phone}    → 每日发送计数（TTL=24h）
 * verification:ip:limit:phone:{ip}    → IP 发送频率（TTL=60s）
 * verification:verify:fail:{type}:{target} → 验证失败计数（TTL=600s）
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    /**
     * Redis 模板，用于存储验证码和限流计数
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 邮件发送器（Spring Mail）
     */
    private final JavaMailSender mailSender;

    /**
     * WebSocket 处理器，用于推送验证码通知
     */
    private final com.demand.config.WebSocketHandlerImpl webSocketHandler;

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 验证码有效期（秒），默认 300 秒（5 分钟）
     */
    @Value("${verification.code.expiration:300}")
    private Integer codeExpiration;

    /**
     * 验证码长度，默认 6 位数字
     */
    @Value("${verification.code.length:6}")
    private Integer codeLength;

    /**
     * 发送频率限制（秒），默认 60 秒内只能发送 1 次
     */
    @Value("${verification.code.send-limit:60}")
    private Integer sendLimit;

    /**
     * 每日发送上限，默认 5 次
     */
    @Value("${verification.code.daily-limit:5}")
    private Integer dailyLimit;

    /**
     * 是否启用短信模拟模式，默认 true
     * <p>
     * 开发环境下将验证码打印到日志，生产环境需配置真实的短信服务商
     * </p>
     */
    @Value("${sms.mock.enabled:true}")
    private Boolean smsMockEnabled;

    /**
     * 是否启用邮箱模拟模式，默认 false
     * <p>
     * 开发环境下将验证码打印到日志，无需配置 SMTP
     * </p>
     */
    @Value("${email.mock.enabled:false}")
    private Boolean emailMockEnabled;

    /**
     * 是否启用 WebSocket 推送验证码，默认 false
     * <p>
     * 开发环境下可通过 WebSocket 实时推送验证码到前端，提升体验
     * 生产环境建议关闭，避免安全风险
     * </p>
     */
    @Value("${verification.code.websocket.push.enabled:false}")
    private Boolean websocketPushEnabled;

    /**
     * 发件人邮箱地址
     */
    @Value("${spring.mail.username:}")
    private String mailFrom;

    private static final String PHONE_CODE_PREFIX = "verification:phone:";
    private static final String EMAIL_CODE_PREFIX = "verification:email:";
    private static final String SEND_LIMIT_PREFIX = "verification:limit:";
    private static final String DAILY_LIMIT_PREFIX = "verification:daily:";
    private static final String VERIFY_FAIL_LIMIT_PREFIX = "verification:verify:fail:";
    private static final String IP_LIMIT_PREFIX = "verification:ip:limit:";

    /**
     * 生成随机验证码
     * <p>
     * 生成指定位数的纯数字验证码，例如：{@code 123456}
     * </p>
     *
     * @return 验证码字符串
     */
    public String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 发送手机验证码
     * <p>
     * 当前为模拟模式，验证码会打印到日志中。
     * 生产环境需接入阿里云、腾讯云等短信服务商。
     * </p>
     *
     * @param phone    手机号
     * @param clientIp 客户端 IP 地址（用于限流）
     * @throws BusinessException 当手机号格式不正确或触发限流时抛出
     */
    public void sendPhoneCode(String phone, String clientIp) {
        validatePhone(phone);
        checkSendLimit(phone, "phone");
        
        // 检查 IP 限流
        checkIpLimit(clientIp, "phone");

        String code = generateCode();
        if (smsMockEnabled) {
            log.info("【模拟短信】手机号: {}, 验证码: {}, IP: {}", phone, code, clientIp);
            saveCode(phone, code, "phone");
            updateIpLimit(clientIp, "phone"); // 记录 IP 发送次数
            
            // 通过 WebSocket 推送验证码到前端
            pushVerificationCodeToClient(phone, code, "phone");
        } else {
            log.warn("短信服务未配置，请使用模拟模式");
            throw new BusinessException("短信服务未配置");
        }
    }

    /**
     * 发送邮箱验证码
     * <p>
     * 通过 SMTP 协议发送验证码邮件，需在配置文件中设置邮件服务器信息。
     * </p>
     *
     * @param email    邮箱地址
     * @param clientIp 客户端 IP 地址（用于限流）
     * @throws BusinessException 当邮箱格式不正确、发送失败或触发限流时抛出
     */
    public void sendEmailCode(String email, String clientIp) {
        validateEmail(email);
        checkSendLimit(email, "email");
        
        // 检查 IP 限流
        checkIpLimit(clientIp, "email");

        String code = generateCode();
        saveCode(email, code, "email");
        updateIpLimit(clientIp, "email"); // 记录 IP 发送次数

        //开启 SMTP配置发送
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(mailFrom);
//            message.setTo(email);
//            message.setSubject("需求管理平台 - 验证码");
//            message.setText("您的验证码是：" + code + "，有效期5分钟。请勿将验证码泄露给他人。");
//
//            mailSender.send(message);
//            log.info("邮箱验证码发送成功: {}, IP: {}", email, clientIp);
//        } catch (Exception e) {
//            log.error("邮箱验证码发送失败: {}", email, e);
//            throw new BusinessException("验证码发送失败，请稍后重试");
//        }

        // 模拟邮箱验证码发送
        if (emailMockEnabled) {
            log.info("【模拟邮件】邮箱: {}, 验证码: {}, IP: {}", email, code, clientIp);
            
            // 通过 WebSocket 推送验证码到前端
            pushVerificationCodeToClient(email, code, "email");
        } else {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(email);
                message.setSubject("需求管理平台 - 验证码");
                message.setText("您的验证码是：" + code + "，有效期5分钟。请勿将验证码泄露给他人。");

                mailSender.send(message);
                log.info("邮箱验证码发送成功: {}, IP: {}", email, clientIp);
            } catch (Exception e) {
                log.error("邮箱验证码发送失败: {}", email, e);
                throw new BusinessException("验证码发送失败，请稍后重试");
            }
        }
    }

    /**
     * 验证验证码
     * <p>
     * 验证流程：
     * 1. 检查验证失败次数，超过 5 次则锁定 10 分钟
     * 2. 从 Redis 中获取存储的验证码
     * 3. 比对用户输入的验证码
     * 4. 验证成功后删除 Redis 中的验证码和失败计数
     * </p>
     *
     * @param target 目标（手机号或邮箱）
     * @param code   用户输入的验证码
     * @param type   类型（"phone" 或 "email"）
     * @return true 表示验证成功，false 表示失败
     * @throws BusinessException 当验证失败次数过多时抛出
     */
    public boolean verifyCode(String target, String code, String type) {
        String failLimitKey = VERIFY_FAIL_LIMIT_PREFIX + type + ":" + target;
        String failCountStr = redisTemplate.opsForValue().get(failLimitKey);
        
        // 检查验证失败次数
        if (failCountStr != null && Integer.parseInt(failCountStr) >= 5) {
            log.warn("验证码验证失败次数过多: {}={}", type, target);
            throw new BusinessException("验证码验证失败次数过多，请重新获取");
        }

        String key = (type.equals("phone") ? PHONE_CODE_PREFIX : EMAIL_CODE_PREFIX) + target;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("验证码不存在或已过期: {}", target);
            return false;
        }

        if (storedCode.equals(code)) {
            // 验证成功，删除验证码和失败计数
            redisTemplate.delete(key);
            redisTemplate.delete(failLimitKey);
            log.info("验证码验证成功: {}={}", type, target);
            return true;
        }

        // 验证失败，累加失败计数
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) + 1 : 1;
        redisTemplate.opsForValue().set(failLimitKey, String.valueOf(failCount), 10, TimeUnit.MINUTES);
        log.warn("验证码验证失败: {}={}, 失败次数: {}", type, target, failCount);
        return false;
    }

    /**
     * 保存验证码到 Redis
     * <p>
     * 同时更新发送频率限制和每日计数。
     * </p>
     *
     * @param target 目标（手机号或邮箱）
     * @param code   验证码
     * @param type   类型（"phone" 或 "email"）
     */
    private void saveCode(String target, String code, String type) {
        String key = (type.equals("phone") ? PHONE_CODE_PREFIX : EMAIL_CODE_PREFIX) + target;
        redisTemplate.opsForValue().set(key, code, codeExpiration, TimeUnit.SECONDS);

        updateSendLimit(target, type);

        log.debug("验证码已保存: {}={}, 有效期={}秒", type, target, codeExpiration);
    }

    /**
     * 检查发送频率限制
     * <p>
     * 验证两个条件：
     * 1. 60 秒内是否已发送过
     * 2. 今日发送次数是否达到上限
     * </p>
     *
     * @param target 目标（手机号或邮箱）
     * @param type   类型
     * @throws BusinessException 当触发限流时抛出
     */
    private void checkSendLimit(String target, String type) {
        String limitKey = SEND_LIMIT_PREFIX + type + ":" + target;
        String dailyKey = DAILY_LIMIT_PREFIX + type + ":" + target;

        String limitValue = redisTemplate.opsForValue().get(limitKey);
        if (limitValue != null) {
            throw new BusinessException("验证码发送过于频繁，请1分钟后再试");
        }

        String dailyValue = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = dailyValue != null ? Integer.parseInt(dailyValue) : 0;
        if (dailyCount >= dailyLimit) {
            throw new BusinessException("今日验证码发送次数已达上限，请明天再试");
        }
    }

    /**
     * 更新发送频率限制
     * <p>
     * 设置 60 秒内的发送标记，并累加每日计数。
     * </p>
     *
     * @param target 目标
     * @param type   类型
     */
    private void updateSendLimit(String target, String type) {
        String limitKey = SEND_LIMIT_PREFIX + type + ":" + target;
        String dailyKey = DAILY_LIMIT_PREFIX + type + ":" + target;

        redisTemplate.opsForValue().set(limitKey, "1", sendLimit, TimeUnit.SECONDS);

        if (!redisTemplate.hasKey(dailyKey)) {
            redisTemplate.opsForValue().set(dailyKey, "1", 24, TimeUnit.HOURS);
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }
    }

    /**
     * 检查 IP 维度的发送频率
     * <p>
     * 同一 IP 在 60 秒内最多发送 3 次验证码，防止恶意刷接口。
     * </p>
     *
     * @param ip   客户端 IP
     * @param type 类型
     * @throws BusinessException 当 IP 发送过于频繁时抛出
     */
    private void checkIpLimit(String ip, String type) {
        String ipLimitKey = IP_LIMIT_PREFIX + type + ":" + ip;
        String ipCountStr = redisTemplate.opsForValue().get(ipLimitKey);
        int ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;

        if (ipCount >= 3) {
            log.warn("IP 发送过于频繁: ip={}, type={}", ip, type);
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
    }

    /**
     * 更新 IP 发送计数
     *
     * @param ip   客户端 IP
     * @param type 类型
     */
    private void updateIpLimit(String ip, String type) {
        String ipLimitKey = IP_LIMIT_PREFIX + type + ":" + ip;
        if (!redisTemplate.hasKey(ipLimitKey)) {
            redisTemplate.opsForValue().set(ipLimitKey, "1", 60, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(ipLimitKey);
        }
    }

    /**
     * 验证手机号格式
     * <p>
     * 正则表达式：{@code ^1[3-9]\d{9}$}
     * 匹配中国大陆 11 位手机号，号段为 13x-19x。
     * </p>
     *
     * @param phone 手机号
     * @throws BusinessException 当格式不正确时抛出
     */
    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
    }

    /**
     * 验证邮箱格式
     * <p>
     * 正则表达式：{@code ^[\w-\.]+@[\w-\.]+\.[a-z]{2,}$}
     * 匹配常见的邮箱格式，如 user@example.com。
     * </p>
     *
     * @param email 邮箱地址
     * @throws BusinessException 当格式不正确时抛出
     */
    private void validateEmail(String email) {
        if (email == null || !email.matches("^[\\w-\\.]+@[\\w-\\.]+\\.[a-z]{2,}$")) {
            throw new BusinessException("邮箱格式不正确");
        }
    }

    /**
     * 通过 WebSocket 推送验证码到前端
     * <p>
     * 将验证码实时推送到客户端，提升用户体验，避免查看日志。
     * 仅在模拟模式下且开启 WebSocket 推送时生效。
     * </p>
     *
     * @param target   目标（手机号或邮箱）
     * @param code     验证码
     * @param type     类型（"phone" 或 "email"）
     */
    private void pushVerificationCodeToClient(String target, String code, String type) {
        if (!websocketPushEnabled) {
            log.debug("WebSocket 推送验证码功能已禁用");
            return;
        }

        try {
            // 脱敏处理：只显示部分信息
            String maskedTarget = maskTarget(target, type);
            
            VerificationCodeNotification notification = VerificationCodeNotification.builder()
                    .type("VERIFICATION_CODE")
                    .code(code)
                    .codeType(type)
                    .target(maskedTarget)
                    .expiration(codeExpiration)
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            String message = objectMapper.writeValueAsString(notification);
            
            // 广播给所有连接的客户端
            webSocketHandler.broadcast(message);
            
            log.info("✅ 验证码已通过 WebSocket 推送: {}={}, 验证码: {}", type, maskedTarget, code);
        } catch (Exception e) {
            log.error("❌ WebSocket 推送验证码失败", e);
            // 推送失败不影响验证码发送，只是降级为日志方式
        }
    }

    /**
     * 脱敏处理目标信息
     * <p>
     * 手机号：138****5678<br>
     * 邮箱：zh***@example.com
     * </p>
     *
     * @param target 目标（手机号或邮箱）
     * @param type   类型（"phone" 或 "email"）
     * @return 脱敏后的字符串
     */
    private String maskTarget(String target, String type) {
        if ("phone".equals(type)) {
            // 手机号脱敏：138****5678
            if (target != null && target.length() == 11) {
                return target.substring(0, 3) + "****" + target.substring(7);
            }
            return target;
        } else if ("email".equals(type)) {
            // 邮箱脱敏：zh***@example.com
            if (target != null) {
                int atIndex = target.indexOf("@");
                if (atIndex > 2) {
                    return target.substring(0, 2) + "***" + target.substring(atIndex);
                }
            }
            return target;
        }
        return target;
    }
}
