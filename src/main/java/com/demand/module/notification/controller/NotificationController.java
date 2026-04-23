package com.demand.module.notification.controller;

import com.demand.common.PageResult;
import com.demand.common.Result;
import com.demand.module.notification.entity.Notification;
import com.demand.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知消息控制器
 * <p>
 * 提供系统通知的查询、标记已读等交互功能。
 * 通知用于实时告知用户系统中的重要事件，如需求审批结果、任务分配、评论回复等。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>通知查询</b>：分页查询当前用户的通知列表</li>
 *   <li><b>未读统计</b>：获取未读消息数量，用于前端角标显示</li>
 *   <li><b>标记已读</b>：单条、批量或全部标记为已读</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>需求审批通知：项目经理审批通过后通知提交人</li>
 *   <li>任务分配通知：负责人被分配新任务时收到提醒</li>
 *   <li>评论回复通知：有人回复你的评论时收到通知</li>
 *   <li>状态变更通知：需求状态从"开发中"变为"已完成"</li>
 * </ul>
 * 
 * <h3>实时推送：</h3>
 * <p>
 * 新通知会通过 WebSocket 实时推送到前端，无需刷新页面即可看到最新消息。
 * 如果用户不在线，通知会存储在数据库中，下次登录时自动加载。
 * </p>
 */
@Tag(name = "通知管理", description = "系统通知消息的查询和标记接口")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    /**
     * 通知业务逻辑服务
     */
    private final NotificationService notificationService;

    /**
     * 分页查询通知列表
     * <p>
     * 返回当前登录用户的所有通知，按创建时间倒序排列（最新的在前）。
     * 支持分页加载，避免一次性返回过多数据影响性能。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>前端点击通知图标时加载第一页数据</li>
     *   <li>滚动到底部时自动加载下一页（无限滚动）</li>
     * </ul>
     * </p>
     *
     * @param pageNum  页码（从 1 开始，默认 1）
     * @param pageSize 每页条数（默认 10，最大 100）
     * @param request  HTTP 请求对象（从中提取 userId）
     * @return 分页结果（包含通知列表和总记录数）
     */
    @Operation(summary = "查询通知列表", description = "分页查询当前用户的通知消息")
    @GetMapping
    public Result<PageResult<Notification>> getNotifications(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量，默认10") @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        Long receiverId = (Long) request.getAttribute("userId");
        PageResult<Notification> pageResult = notificationService.getNotifications(receiverId, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询未读通知数量
     * <p>
     * 用于前端在通知图标上显示红色角标，提示用户有未读消息。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>页面加载时调用，显示未读数量</li>
     *   <li>收到 WebSocket 推送后重新查询，更新角标</li>
     * </ul>
     * </p>
     *
     * @param request HTTP 请求对象（从中提取 userId）
     * @return 未读通知数量
     */
    @Operation(summary = "查询未读数量", description = "获取当前用户的未读消息数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Long receiverId = (Long) request.getAttribute("userId");
        Long count = notificationService.getUnreadCount(receiverId);
        return Result.success(count);
    }

    /**
     * 标记单条通知为已读
     * <p>
     * 用户点击某条通知后调用此接口，将该通知的 {@code is_read} 字段设置为 1。
     * </p>
     *
     * @param id 通知 ID
     * @return 操作结果
     */
    @Operation(summary = "标记为已读", description = "将指定通知标记为已读")
    @PutMapping("/{id}/read")
    public Result<?> markAsRead(@Parameter(description = "通知ID") @PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }

    /**
     * 全部标记为已读
     * <p>
     * 一键将所有未读通知标记为已读，常用于"全部已读"按钮。
     * </p>
     *
     * @param request HTTP 请求对象（从中提取 userId）
     * @return 操作结果
     */
    @Operation(summary = "全部标记为已读", description = "将当前用户的所有通知标记为已读")
    @PutMapping("/read-all")
    public Result<?> markAllAsRead(HttpServletRequest request) {
        Long receiverId = (Long) request.getAttribute("userId");
        notificationService.markAllAsRead(receiverId);
        return Result.success();
    }

    /**
     * 批量标记为已读
     * <p>
     * 选中多条通知后批量标记为已读，减少网络请求次数。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>用户勾选多条通知后点击"标记已读"</li>
     *   <li>前端定时批量处理已查看的通知</li>
     * </ul>
     * </p>
     *
     * @param ids 通知 ID 列表
     * @return 操作结果
     */
    @Operation(summary = "批量标记为已读", description = "将选中的通知标记为已读")
    @PutMapping("/read-batch")
    public Result<?> markBatchAsRead(@RequestBody List<Long> ids) {
        notificationService.markBatchAsRead(ids);
        return Result.success();
    }
}
