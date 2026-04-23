package com.demand.module.comment.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.user.service.PermissionService;
import com.demand.module.comment.entity.Comment;
import com.demand.module.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理控制器
 * <p>
 * 提供需求评论的添加、查询和删除功能。
 * 支持嵌套回复（通过 parentId 字段），实现类似社交媒体的评论系统。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>添加评论</b>：为需求添加新评论或回复他人评论</li>
 *   <li><b>查询评论</b>：按时间顺序返回需求的所有评论</li>
 *   <li><b>删除评论</b>：仅允许评论作者或管理员删除</li>
 * </ul>
 * 
 * <h3>权限控制：</h3>
 * <ul>
 *   <li><b>添加评论</b>：普通用户（role=1）、项目经理（role=3）、管理员（role=2）</li>
 *   <li><b>删除评论</b>：仅评论作者本人或管理员可删除（通过 {@link RequirePermission} 注解实现）</li>
 * </ul>
 * 
 * <h3>通知机制：</h3>
 * <p>
 * 当用户发表评论时，系统会自动发送通知给：
 * <ul>
 *   <li>需求提出人：收到"新评论"通知</li>
 *   <li>需求负责人：收到"新评论"通知</li>
 * </ul>
 * 通知通过 WebSocket 实时推送，确保相关人员及时获知讨论动态。
 * </p>
 */
@Tag(name = "评论管理", description = "需求评论的增删查接口")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    /**
     * 评论业务逻辑服务
     */
    private final CommentService commentService;

    /**
     * 权限验证服务，用于获取当前用户 ID
     */
    private final PermissionService permissionService;

    /**
     * 添加评论
     * <p>
     * 为指定需求添加新评论或回复他人评论。
     * 添加成功后会自动通知需求提出人和负责人。
     * </p>
     * 
     * <p>
     * <b>权限要求</b>：
     * <ul>
     *   <li>普通用户（role=1）：可以评论</li>
     *   <li>项目经理（role=3）：可以评论</li>
     *   <li>管理员（role=2）：可以评论</li>
     *   <li>只读用户（role=0）：无权评论</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>评论类型</b>：
     * <ul>
     *   <li>一级评论：parentId 为 null，直接评论需求</li>
     *   <li>二级回复：parentId 不为 null，回复某条评论</li>
     * </ul>
     * </p>
     *
     * @param comment 评论对象（包含 demandId、content、parentId 等字段）
     * @return 评论信息（包含自动生成的 ID 和时间戳）
     * @throws BusinessException 当用户无权限或参数无效时抛出
     */
    @Operation(summary = "添加评论", description = "为需求添加评论或回复")
    @PostMapping
    @RequirePermission(roles = {1, 2, 3})
    public Result<Comment> addComment(@RequestBody Comment comment) {
        // 1. 获取当前登录用户 ID
        Long currentUserId = permissionService.getCurrentUserId();
        
        // 2. 设置评论人 ID
        comment.setUserId(currentUserId);
        
        // 3. 保存评论并发送通知
        commentService.addComment(comment);
        return Result.success(comment);
    }

    /**
     * 查询需求的评论列表
     * <p>
     * 返回指定需求的所有评论，按创建时间升序排列（最早的在前）。
     * 每条评论包含评论人姓名和头像，方便前端展示。
     * </p>
     * 
     * <p>
     * <b>返回数据</b>：
     * <ul>
     *   <li>评论 ID、内容、创建时间</li>
     *   <li>评论人姓名（userName）和头像（userAvatar）</li>
     *   <li>父评论 ID（parentId），用于构建嵌套结构</li>
     * </ul>
     * </p>
     *
     * @param demandId 需求 ID
     * @return 评论列表
     */
    @Operation(summary = "查询评论列表", description = "查询指定需求下的所有评论")
    @GetMapping("/demand/{demandId}")
    public Result<List<Comment>> getComments(@Parameter(description = "需求ID") @PathVariable Long demandId) {
        List<Comment> comments = commentService.getCommentsByDemandId(demandId);
        return Result.success(comments);
    }

    /**
     * 删除评论
     * <p>
     * 删除指定的评论记录。
     * 只有评论作者本人或管理员可以删除评论。
     * </p>
     * 
     * <p>
     * <b>权限控制</b>：
     * 使用 {@link RequirePermission(requireOwner = true)} 注解，自动校验：
     * <ul>
     *   <li>当前用户是否为评论作者（userId 匹配）</li>
     *   <li>或者当前用户是否为管理员（role=2）</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>注意</b>：删除评论不会级联删除子回复，子回复的 parentId 将指向不存在的记录。
     * 建议前端在删除前提示用户确认。
     * </p>
     *
     * @param id 评论 ID
     * @return 操作结果
     * @throws BusinessException 当评论不存在或用户无权限时抛出
     */
    @Operation(summary = "删除评论", description = "删除指定评论")
    @DeleteMapping("/{id}")
    @RequirePermission(requireOwner = true, resourceIdParam = "id")
    public Result<?> deleteComment(@Parameter(description = "评论ID") @PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
