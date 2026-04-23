package com.demand.module.user.controller;

import com.demand.common.Result;
import com.demand.module.user.service.VerificationCodeService;
import com.demand.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 验证码控制器
 * <p>
 * 提供手机和邮箱验证码的发送功能，用于用户注册、登录等场景的身份验证。
 * 内置多重限流保护，防止短信/邮件轰炸攻击。
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
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>用户注册时验证邮箱所有权</li>
 *   <li>邮箱/手机验证码登录</li>
 *   <li>忘记密码时验证身份</li>
 * </ul>
 */
@Tag(name = "验证码管理", description = "短信和邮箱验证码接口")
@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    /**
     * 验证码业务逻辑服务
     */
    private final VerificationCodeService verificationCodeService;

    /**
     * 手机号请求对象
     */
    @Data
    static class PhoneRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;
    }

    /**
     * 邮箱请求对象
     */
    @Data
    static class EmailRequest {
        @NotBlank(message = "邮箱不能为空")
        private String email;
    }

    /**
     * 发送手机验证码
     * <p>
     * 当前为模拟模式（{@code sms.mock.enabled=true}），验证码会打印到日志中。
     * 生产环境需配置真实的短信服务商（如阿里云、腾讯云）。
     * </p>
     * 
     * <p>
     * <b>限流保护</b>：
     * <ul>
     *   <li>同一手机号 60 秒内只能发送 1 次</li>
     *   <li>同一 IP 60 秒内最多发送 3 次</li>
     * </ul>
     * </p>
     *
     * @param request HTTP 请求对象（用于获取客户端 IP）
     * @param req     包含手机号的请求对象
     * @return 操作结果
     */
    @Operation(summary = "发送手机验证码")
    @PostMapping("/phone")
    public Result<String> sendPhoneCode(HttpServletRequest request, @Valid @RequestBody PhoneRequest req) {
        verificationCodeService.sendPhoneCode(req.getPhone(), IpUtil.getClientIp(request));
        return Result.success("验证码已发送");
    }

    /**
     * 发送邮箱验证码
     * <p>
     * 通过 SMTP 协议发送验证码邮件，需在 {@code application.properties} 中配置邮件服务器信息。
     * </p>
     * 
     * <p>
     * <b>限流保护</b>：
     * <ul>
     *   <li>同一邮箱 60 秒内只能发送 1 次</li>
     *   <li>同一 IP 60 秒内最多发送 3 次</li>
     * </ul>
     * </p>
     *
     * @param request HTTP 请求对象（用于获取客户端 IP）
     * @param req     包含邮箱的请求对象
     * @return 操作结果
     */
    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email")
    public Result<?> sendEmailCode(HttpServletRequest request, @Valid @RequestBody EmailRequest req) {
        verificationCodeService.sendEmailCode(req.getEmail(), IpUtil.getClientIp(request));
        return Result.success("验证码已发送");
    }
}
