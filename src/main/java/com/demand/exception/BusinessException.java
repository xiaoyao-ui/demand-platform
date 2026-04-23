package com.demand.exception;

import lombok.Getter;

/**
 * 业务逻辑异常类
 * <p>
 * 继承自 {@link RuntimeException}，用于在 Service 层抛出业务相关的错误。
 * 通过 {@link GlobalExceptionHandler} 统一捕获并转换为标准的 JSON 响应格式。
 * </p>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>参数校验失败：如"需求标题不能为空"</li>
 *   <li>资源不存在：如"需求 ID 123 不存在"</li>
 *   <li>权限不足：如"您没有权限删除此需求"</li>
 *   <li>状态冲突：如"只能删除草稿状态的需求"</li>
 *   <li>业务规则违反：如"审批通过的需求不能再修改"</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 简单错误（默认 code = 500）
 * throw new BusinessException("需求不存在");
 * 
 * // 自定义错误码
 * throw new BusinessException(403, "权限不足");
 * throw new BusinessException(404, "资源不存在");
 * }</pre>
 * 
 * <p>
 * <b>为什么继承 RuntimeException？</b><br>
 * 非受检异常（Unchecked Exception）不需要在方法签名中声明 {@code throws}，
 * 使得代码更加简洁，符合 Spring Boot 的最佳实践。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     * <p>
     * 常见错误码：
     * <ul>
     *   <li>400 - 请求参数错误</li>
     *   <li>401 - 未登录或 Token 无效</li>
     *   <li>403 - 权限不足</li>
     *   <li>404 - 资源不存在</li>
     *   <li>500 - 服务器内部错误（默认值）</li>
     * </ul>
     * </p>
     */
    private Integer code;

    /**
     * 构造业务异常（默认错误码 500）
     * <p>
     * 适用于一般的业务错误，前端会显示错误消息。
     * </p>
     *
     * @param message 错误描述信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造业务异常（自定义错误码）
     * <p>
     * 适用于需要区分错误类型的场景，前端可根据 code 执行不同的逻辑。
     * </p>
     * 
     * <p>
     * <b>示例：</b>
     * <ul>
     *   <li>{@code new BusinessException(401, "Token 已过期")} → 前端跳转到登录页</li>
     *   <li>{@code new BusinessException(403, "无权限访问")} → 前端显示权限提示</li>
     * </ul>
     * </p>
     *
     * @param code    错误码
     * @param message 错误描述信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
