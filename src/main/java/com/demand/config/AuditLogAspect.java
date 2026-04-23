package com.demand.config;

import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import com.demand.module.user.entity.OperationLog;
import com.demand.module.user.mapper.OperationLogMapper;
import com.demand.module.user.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作审计日志切面类
 * <p>
 * 使用 AOP 技术自动记录所有写操作（POST/PUT/DELETE）的执行情况。
 * 记录内容包括：操作人、请求路径、请求参数、IP 地址、执行状态等。
 * 日志数据持久化到 operation_log 表，用于安全审计和问题追踪。
 * </p>
 * <p>
 * 核心特性：
 * 1. 非侵入式设计：通过切面自动记录，业务代码无需手动写日志
 * 2. 异常安全：即使日志记录失败也不影响主业务逻辑
 * 3. 智能过滤：自动过滤 Servlet 原生对象，避免序列化报错
 * </p>
 *
 * @author demand-platform
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final PermissionService permissionService;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 定义切点：拦截所有 POST、PUT、DELETE 请求
     * <p>
     * 这些请求通常代表数据的增、改、删操作，需要进行审计记录。
     * GET 请求一般用于查询，不会修改数据，故不做记录。
     * </p>
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void writeOperations() {
    }

    /**
     * 正常返回后记录日志
     *
     * @param joinPoint 连接点，包含目标方法的信息
     * @param result    方法返回值
     */
    @AfterReturning(pointcut = "writeOperations()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            saveOperationLog(joinPoint, result, null);
        } catch (Exception e) {
            // 日志记录失败不应影响主业务，仅打印调试信息
            log.debug("审计日志记录失败：{}", e.getMessage());
        }
    }

    /**
     * 抛出异常后记录日志
     *
     * @param joinPoint 连接点，包含目标方法的信息
     * @param exception 捕获到的异常信息
     */
    @AfterThrowing(pointcut = "writeOperations()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        try {
            saveOperationLog(joinPoint, null, exception.getMessage());
        } catch (Exception e) {
            log.debug("审计日志记录失败：{}", e.getMessage());
        }
    }

    /**
     * 保存操作日志到数据库
     *
     * @param joinPoint 连接点
     * @param result    方法返回值（为 null 表示执行失败）
     * @param errorMsg  错误信息（为 null 表示执行成功）
     */
    private void saveOperationLog(JoinPoint joinPoint, Object result, String errorMsg) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return;
        }

        OperationLog operationLog = new OperationLog();
        
        try {
            // 获取当前登录用户信息
            Long userId = permissionService.getCurrentUserId();
            operationLog.setUserId(userId);
            if (userId != null) {
                User user = userMapper.findById(userId);
                if (user != null) {
                    operationLog.setUsername(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            }
        } catch (Exception e) {
            // 如果获取用户信息失败，不阻塞日志记录
            operationLog.setUserId(null);
        }
        
        // 设置请求基本信息
        operationLog.setOperation(joinPoint.getSignature().getName());
        operationLog.setMethod(request.getMethod());
        operationLog.setUri(request.getRequestURI());
        operationLog.setParams(getRequestParams(joinPoint.getArgs()));
        operationLog.setIp(getClientIp(request));
        operationLog.setStatus(result != null ? 200 : 500);
        operationLog.setErrorMsg(errorMsg);
        operationLog.setCreateTime(LocalDateTime.now());

        // 持久化日志
        operationLogMapper.insert(operationLog);
        
        log.info("操作日志已保存：userId={}, uri={}, status={}", 
                operationLog.getUserId(), operationLog.getUri(), operationLog.getStatus());
    }

    /**
     * 获取当前请求的 HttpServletRequest 对象
     *
     * @return HttpServletRequest 对象，若不在请求上下文中则返回 null
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 将方法参数序列化为 JSON 字符串
     * <p>
     * 过滤掉 HttpServletRequest、HttpServletResponse、Model、BindingResult 等
     * 无法序列化或体积过大的对象，仅保留业务参数。
     * </p>
     *
     * @param args 方法参数数组
     * @return JSON 格式的参数串
     */
    private String getRequestParams(Object[] args) {
        if (args == null || args.length == 0) return "";
        
        try {
            List<Object> validArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) &&
                               !(arg instanceof HttpServletResponse) &&
                               !(arg instanceof Model) &&
                               !(arg instanceof BindingResult))
                .collect(Collectors.toList());
            
            if (!validArgs.isEmpty()) {
                return objectMapper.writeValueAsString(validArgs);
            }
        } catch (Exception e) {
            log.debug("序列化请求参数失败", e);
        }
        return Arrays.toString(args);
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 优先从 X-Forwarded-For 请求头获取（适用于经过 Nginx 反向代理的场景），
     * 若获取不到则使用 getRemoteAddr() 方法。
     * </p>
     *
     * @param request 请求对象
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
