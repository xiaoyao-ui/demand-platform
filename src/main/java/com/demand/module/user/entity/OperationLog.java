package com.demand.module.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * <p>
 * 对应数据库 {@code operation_log} 表，记录系统中所有关键操作的详细信息。
 * 通过 AOP 切面（{@link com.demand.config.AuditLogAspect}）自动捕获并保存。
 * </p>
 * 
 * <h3>记录内容：</h3>
 * <ul>
 *   <li><b>用户信息</b>：userId、username（谁操作的）</li>
 *   <li><b>请求信息</b>：method、uri、params（做了什么操作）</li>
 *   <li><b>执行结果</b>：status、errorMsg（成功还是失败）</li>
 *   <li><b>环境信息</b>：ip、createTime（何时何地操作的）</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>审计追踪：查看某用户在特定时间段的所有操作</li>
 *   <li>故障排查：定位异常请求的参数和堆栈信息</li>
 *   <li>安全监控：检测异常登录、暴力破解等可疑行为</li>
 *   <li>合规要求：满足数据安全法规的日志留存要求（保留 90 天）</li>
 * </ul>
 */
@Data
public class OperationLog {

    /**
     * 日志 ID（主键）
     */
    private Long id;

    /**
     * 操作用户 ID
     * <p>
     * 未登录用户为 null（如访问公开接口）
     * </p>
     */
    private Long userId;

    /**
     * 操作用户名
     * <p>
     * 从 JWT Token 或 Session 中提取，方便快速检索
     * </p>
     */
    private String username;

    /**
     * 操作描述
     * <p>
     * 例如："创建需求"、"删除用户"、"修改密码"
     * 由 {@link com.demand.config.AuditLogAspect} 根据请求路径和方法自动生成
     * </p>
     */
    private String operation;

    /**
     * HTTP 请求方法
     * <ul>
     *   <li>GET - 查询</li>
     *   <li>POST - 创建</li>
     *   <li>PUT - 更新</li>
     *   <li>DELETE - 删除</li>
     * </ul>
     */
    private String method;

    /**
     * 请求 URI
     * <p>
     * 例如：/api/demand/create、/api/user/login
     * </p>
     */
    private String uri;

    /**
     * 请求参数（JSON 格式）
     * <p>
     * GET 请求：URL 查询参数<br>
     * POST/PUT 请求：请求体（Request Body）
     * </p>
     * 
     * <p>
     * <b>注意</b>：敏感字段（如密码）会被脱敏处理，不会明文存储
     * </p>
     */
    private String params;

    /**
     * 客户端 IP 地址
     * <p>
     * 通过 {@link com.demand.util.IpUtil#getClientIp} 获取，支持代理环境
     * </p>
     */
    private String ip;

    /**
     * HTTP 响应状态码
     * <ul>
     *   <li>200 - 成功</li>
     *   <li>400 - 请求参数错误</li>
     *   <li>401 - 未登录或 Token 无效</li>
     *   <li>403 - 权限不足</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     */
    private Integer status;

    /**
     * 错误消息
     * <p>
     * 当 status != 200 时，记录异常信息或业务错误提示。
     * 成功时为 null。
     * </p>
     */
    private String errorMsg;

    /**
     * 操作时间
     * <p>
     * 精确到毫秒，用于时序分析和审计追踪
     * </p>
     */
    private LocalDateTime createTime;
}
