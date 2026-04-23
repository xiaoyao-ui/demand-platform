package com.demand.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 邮箱脱敏序列化器
 * <p>
 * 继承自 Jackson 的 {@link JsonSerializer}，用于在 JSON 响应中自动对邮箱地址进行脱敏处理。
 * 保护用户隐私，防止敏感信息泄露。
 * </p>
 * 
 * <h3>脱敏规则：</h3>
 * <ul>
 *   <li><b>用户名长度 ≤ 2</b>：保留首字符，其余用 {@code ***} 替代<br>
 *       例如：{@code ab@example.com} → {@code a***@example.com}</li>
 *   
 *   <li><b>用户名长度 > 2</b>：保留前两个字符，其余用 {@code ***} 替代<br>
 *       例如：{@code zhangsan@example.com} → {@code zh***@example.com}</li>
 *   
 *   <li><b>无效邮箱</b>：不进行脱敏，直接返回原值（包括 null）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public class User {
 *     @JsonSerialize(using = EmailSerializer.class)
 *     private String email;
 * }
 * 
 * // JSON 响应：{"email": "zh***@example.com"}
 * }</pre>
 * 
 * <p>
 * <b>应用场景：</b>
 * <ul>
 *   <li>用户列表接口：展示部分用户信息时</li>
 *   <li>个人资料页：非本人查看时</li>
 *   <li>日志记录：避免明文存储敏感数据</li>
 * </ul>
 * </p>
 */
public class EmailSerializer extends JsonSerializer<String> {

    /**
     * 执行邮箱脱敏序列化
     * <p>
     * 工作流程：
     * 1. 校验邮箱格式（必须包含 {@code @} 符号）
     * 2. 分割用户名和域名部分
     * 3. 根据用户名长度应用不同的脱敏策略
     * 4. 将脱敏后的字符串写入 JSON 输出流
     * </p>
     *
     * @param email          原始邮箱地址
     * @param gen            JSON 生成器
     * @param serializers    序列化提供者
     * @throws IOException   IO 异常
     */
    @Override
    public void serialize(String email, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 1. 校验邮箱有效性
        if (email == null || !email.contains("@")) {
            gen.writeString(email);
            return;
        }

        // 2. 分割用户名和域名
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        // 3. 根据用户名长度应用脱敏规则
        if (username.length() <= 2) {
            // 短用户名：保留首字符
            gen.writeString(username.charAt(0) + "***@" + domain);
        } else {
            // 长用户名：保留前两个字符
            gen.writeString(username.substring(0, 2) + "***@" + domain);
        }
    }
}
