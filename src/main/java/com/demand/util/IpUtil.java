package com.demand.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址获取工具类
 * <p>
 * 从 HTTP 请求中提取客户端真实 IP 地址，支持多层代理和负载均衡场景。
 * 主要用于审计日志、限流控制、地理位置分析等功能。
 * </p>
 * 
 * <h3>IP 获取优先级：</h3>
 * <ol>
 *   <li><b>{@code X-Forwarded-For}</b>：标准代理头，记录完整的代理链路<br>
 *       格式：{@code client, proxy1, proxy2}（取第一个为真实 IP）</li>
 *   
 *   <li><b>{@code Proxy-Client-IP}</b>：Apache 代理服务器的自定义头</li>
 *   
 *   <li><b>{@code WL-Proxy-Client-IP}</b>：WebLogic 服务器的自定义头</li>
 *   
 *   <li><b>{@code RemoteAddr}</b>：直连时的 TCP 连接 IP（最后一道防线）</li>
 * </ol>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>审计日志：记录操作人的 IP 地址</li>
 *   <li>接口限流：基于 IP 限制访问频率（{@link RateLimitInterceptor}）</li>
 *   <li>安全防护：检测异常登录地点</li>
 *   <li>数据分析：统计用户地域分布</li>
 * </ul>
 * 
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>在 Nginx 反向代理环境下，需配置 {@code proxy_set_header X-Forwarded-For $remote_addr;}</li>
 *   <li>{@code X-Forwarded-For} 可能被伪造，安全性要求高的场景应结合其他验证手段</li>
 * </ul>
 * </p>
 */
public class IpUtil {

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 工作流程：
     * 1. 依次尝试从多个 HTTP Header 中提取 IP
     * 2. 如果都为空或 "unknown"，则使用 {@code request.getRemoteAddr()}
     * 3. 处理多层代理情况：从 {@code X-Forwarded-For} 中提取第一个 IP
     * </p>
     * 
     * <p>
     * <b>为什么需要检查多个 Header？</b><br>
     * 不同的代理服务器和负载均衡器使用的 Header 名称不同：
     * <ul>
     *   <li>Nginx/HAProxy：{@code X-Forwarded-For}</li>
     *   <li>Apache：{@code Proxy-Client-IP}</li>
     *   <li>WebLogic：{@code WL-Proxy-Client-IP}</li>
     * </ul>
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端真实 IP 地址，如果无法获取则返回 {@code null}
     */
    public static String getClientIp(HttpServletRequest request) {
        // 1. 尝试从 X-Forwarded-For 获取（最常用）
        String ip = request.getHeader("X-Forwarded-For");
        
        // 2. 尝试从 Proxy-Client-IP 获取（Apache）
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        // 3. 尝试从 WL-Proxy-Client-IP 获取（WebLogic）
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        // 4. 降级到 RemoteAddr（直连或所有代理头都无效时）
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 5. 处理多层代理情况：X-Forwarded-For 可能包含多个 IP（client, proxy1, proxy2）
        //    第一个 IP 为真实客户端 IP
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
