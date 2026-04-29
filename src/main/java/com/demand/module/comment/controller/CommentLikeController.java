package com.demand.module.comment.controller;

import com.demand.common.Result;
import com.demand.config.RequirePermission;
import com.demand.module.comment.service.CommentLikeService;
import com.demand.module.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论点赞控制器
 * <p>
 * 提供评论的点赞和取消点赞功能。
 * </p>
 */
@Tag(name = "评论点赞", description = "评论点赞和取消点赞接口")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;
    private final PermissionService permissionService;

    /**
     * 点赞评论
     * <p>
     * 用户对指定评论进行点赞。如果已经点过赞，则返回错误。
     * </p>
     *
     * @param commentId 评论 ID
     * @return 操作结果
     */
    @Operation(summary = "点赞评论", description = "对评论进行点赞")
    @PostMapping("/{commentId}/like")
    @RequirePermission(roles = {1, 2, 3, 4, 5, 6})
    public Result<?> likeComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        Long userId = permissionService.getCurrentUserId();
        commentLikeService.likeComment(commentId, userId);
        return Result.success("点赞成功");
    }

    /**
     * 取消点赞
     * <p>
     * 用户取消对指定评论的点赞。如果未点过赞，则返回错误。
     * </p>
     *
     * @param commentId 评论 ID
     * @return 操作结果
     */
    @Operation(summary = "取消点赞", description = "取消对评论的点赞")
    @DeleteMapping("/{commentId}/like")
    @RequirePermission(roles = {1, 2, 3, 4, 5, 6})
    public Result<?> unlikeComment(@Parameter(description = "评论ID") @PathVariable Long commentId) {
        Long userId = permissionService.getCurrentUserId();
        commentLikeService.unlikeComment(commentId, userId);
        return Result.success("取消点赞成功");
    }
}
