package com.demand.exception;

import com.demand.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 通过 {@link RestControllerAdvice} 统一拦截 Controller 层抛出的所有异常，
 * 并将其转换为标准的 {@link Result} 响应格式，避免前端收到 HTML 错误页面或非结构化数据。
 * </p>
 * 
 * <h3>处理的异常类型：</h3>
 * <ol>
 *   <li><b>{@link BusinessException}</b>：业务逻辑异常（如"需求不存在"、"权限不足"）<br>
 *       → 返回自定义错误码和消息</li>
 *   
 *   <li><b>{@link MethodArgumentNotValidException}</b>：参数校验失败（如 {@code @NotBlank}、{@code @Email} 验证不通过）<br>
 *       → 提取所有字段的错误消息，用分号拼接后返回</li>
 *   
 *   <li><b>{@link Exception}</b>：未预料的系统异常（如空指针、数据库连接失败）<br>
 *       → 记录完整堆栈日志，返回通用错误提示（不暴露技术细节）</li>
 * </ol>
 * 
 * <h3>响应示例：</h3>
 * <pre>{@code
 * // 业务异常
 * {
 *   "code": 403,
 *   "message": "您没有权限删除此需求",
 *   "data": null
 * }
 * 
 * // 参数校验失败
 * {
 *   "code": 400,
 *   "message": "需求标题不能为空; 优先级必须在 0-3 之间",
 *   "data": null
 * }
 * 
 * // 系统异常
 * {
 *   "code": 500,
 *   "message": "系统内部错误，请联系管理员",
 *   "data": null
 * }
 * }</pre>
 * 
 * <p>
 * <b>设计优势：</b>
 * <ul>
 *   <li>统一响应格式：前端只需处理一种数据结构</li>
 *   <li>安全保护：系统异常不暴露堆栈信息，防止泄露技术细节</li>
 *   <li>日志分级：业务异常用 WARN，系统异常用 ERROR，便于问题排查</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务逻辑异常
     * <p>
     * 当 Service 层抛出 {@link BusinessException} 时触发。
     * 通常用于已知的业务错误，如资源不存在、权限不足、状态冲突等。
     * </p>
     * 
     * <p>
     * <b>日志级别：</b>WARN（因为这是预期的业务错误，不是系统故障）
     * </p>
     *
     * @param e 捕获到的业务异常
     * @return 包含错误码和错误消息的 Result 对象
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     * <p>
     * 当 Controller 方法的参数带有 {@code @Valid} 或 {@code @Validated} 注解，
     * 且校验失败时触发（如 {@code @NotBlank}、{@code @Min}、{@code @Email} 等）。
     * </p>
     * 
     * <p>
     * <b>处理逻辑：</b>
     * 1. 提取所有字段错误的默认消息（即注解中的 message 属性）
     * 2. 用分号 + 空格拼接多个错误（如"标题不能为空; 邮箱格式不正确"）
     * 3. 返回 HTTP 400 状态码
     * </p>
     *
     * @param e 捕获到的参数校验异常
     * @return 包含错误码 400 和详细错误消息的 Result 对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数验证失败：{}", message);
        return Result.error(400, message);
    }

    /**
     * 处理未预料的系统异常
     * <p>
     * 兜底处理器，捕获所有未被上述方法处理的异常（如 NullPointerException、SQLException 等）。
     * </p>
     * 
     * <p>
     * <b>安全考虑：</b>
     * <ul>
     *   <li>只返回通用提示"系统内部错误，请联系管理员"，不暴露具体异常信息</li>
     *   <li>记录完整的异常堆栈到日志，方便开发人员排查问题</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>日志级别：</b>ERROR（表示系统出现了非预期故障，需要立即关注）
     * </p>
     *
     * @param e 捕获到的系统异常
     * @return 包含错误码 500 和通用提示的 Result 对象
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}
