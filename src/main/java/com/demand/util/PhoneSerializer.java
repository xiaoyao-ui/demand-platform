package com.demand.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 手机号脱敏序列化器
 * <p>
 * 继承自 Jackson 的 {@link JsonSerializer}，用于在 JSON 响应中自动对手机号码进行脱敏处理。
 * 保护用户隐私，防止敏感信息泄露。
 * </p>
 * 
 * <h3>脱敏规则：</h3>
 * <ul>
 *   <li><b>标准格式</b>：保留前 3 位和后 4 位，中间用 {@code ****} 替代<br>
 *       例如：{@code 13812345678} → {@code 138****5678}</li>
 *   
 *   <li><b>长度不足 7 位</b>：不进行脱敏，直接返回原值（包括 null）<br>
 *       适用于国际号码或特殊格式的兼容处理</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class User {
 *     @JsonSerialize(using = PhoneSerializer.class)
 *     private String phone;
 * }
 * 
 * // JSON 响应：{"phone": "138****5678"}
 * }</pre>
 * 
 * <p>
 * <b>应用场景：</b>
 * <ul>
 *   <li>用户列表接口：展示部分用户信息时</li>
 *   <li>个人资料页：非本人查看时</li>
 *   <li>管理员后台：客服查询用户信息时</li>
 *   <li>日志记录：避免明文存储敏感数据</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <b>合规性说明：</b>
 * 根据《个人信息保护法》和《网络安全法》，手机号属于个人敏感信息，
 * 在非必要场景下应当进行脱敏处理，降低数据泄露风险。
 * </p>
 */
public class PhoneSerializer extends JsonSerializer<String> {

    /**
     * 执行手机号脱敏序列化
     * <p>
     * 工作流程：
     * 1. 校验手机号有效性（长度至少 7 位）
     * 2. 提取前 3 位（号段标识）和后 4 位（用户标识）
     * 3. 中间 4 位用 {@code ****} 替代
     * 4. 将脱敏后的字符串写入 JSON 输出流
     * </p>
     * 
     * <p>
     * <b>为什么保留前 3 后 4？</b><br>
     * - 前 3 位：标识运营商和地区（如 138-中国移动）<br>
     * - 后 4 位：用户的唯一标识符<br>
     * - 中间 4 位：隐藏后可有效防止暴力破解完整号码
     * </p>
     *
     * @param phone          原始手机号码
     * @param gen            JSON 生成器
     * @param serializers    序列化提供者
     * @throws IOException   IO 异常
     */
    @Override
    public void serialize(String phone, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 1. 校验手机号有效性
        if (phone == null || phone.length() < 7) {
            gen.writeString(phone);
            return;
        }

        // 2. 执行脱敏：保留前 3 位 + **** + 后 4 位
        String masked = phone.substring(0, 3) + "****" + phone.substring(7);
        gen.writeString(masked);
    }
}
