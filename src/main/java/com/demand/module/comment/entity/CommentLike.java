package com.demand.module.comment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞实体类
 * <p>
 * 对应数据库 {@code comment_like} 表，记录用户对评论的点赞行为。
 * 使用联合主键（comment_id, user_id）确保同一用户对同一评论只能点赞一次。
 * </p>
 *
 * <h3>典型数据示例：</h3>
 * <pre>
 * +------------+---------+---------------------+
 * | comment_id | user_id | create_time         |
 * +------------+---------+---------------------+
 * |          1 |      10 | 2026-04-23 10:30:00 |
 * |          1 |      20 | 2026-04-23 11:00:00 |
 * |          2 |      10 | 2026-04-23 11:30:00 |
 * +------------+---------+---------------------+
 * </pre>
 */
@Data
public class CommentLike {

    /**
     * 评论 ID（联合主键之一）
     * <p>
     * 关联到 {@code comment.id}，标识被点赞的评论
     * </p>
     */
    private Long commentId;

    /**
     * 点赞用户 ID（联合主键之二）
     * <p>
     * 关联到 {@code user.id}，记录谁点了赞
     * </p>
     */
    private Long userId;

    /**
     * 点赞时间
     * <p>
     * 用户点击点赞按钮的时间，精确到秒
     * </p>
     */
    private LocalDateTime createTime;
}
