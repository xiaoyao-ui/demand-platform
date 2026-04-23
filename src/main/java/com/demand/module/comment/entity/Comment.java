package com.demand.module.comment.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类
 * <p>
 * 对应数据库 {@code comment} 表，存储需求的评论和回复。
 * 支持嵌套回复结构（通过 parentId 字段实现）。
 * </p>
 * 
 * <h3>典型数据示例：</h3>
 * <pre>
 * +----+-----------+---------+----------+------------+---------------------+-----------+---------------------+---------------------+
 * | id | demand_id | user_id | user_name| content    | create_time         | parent_id | update_time         | user_avatar         |
 * +----+-----------+---------+----------+------------+---------------------+-----------+---------------------+---------------------+
 * |  1 |       100 |      10 | 张三     | 这个需求很紧急 | 2026-04-23 10:00:00 |      NULL | 2026-04-23 10:00:00 | /avatar/xxx.jpg     |
 * |  2 |       100 |      20 | 李四     | 收到，马上处理 | 2026-04-23 10:30:00 |         1 | 2026-04-23 10:30:00 | /avatar/yyy.jpg     |
 * |  3 |       100 |      10 | 张三     | 谢谢！      | 2026-04-23 11:00:00 |         2 | 2026-04-23 11:00:00 | /avatar/xxx.jpg     |
 * +----+-----------+---------+----------+------------+---------------------+-----------+---------------------+---------------------+
 * </pre>
 * 
 * <h3>嵌套结构说明：</h3>
 * <ul>
 *   <li><b>一级评论</b>：parentId 为 NULL，直接评论需求</li>
 *   <li><b>二级回复</b>：parentId 指向某条评论的 ID，形成树形结构</li>
 * </ul>
 * 
 * <h3>前端展示建议：</h3>
 * <p>
 * 后端返回扁平列表，前端根据 parentId 构建嵌套树形结构：
 * <pre>{@code
 * [
 *   {
 *     id: 1,
 *     content: "这个需求很紧急",
 *     children: [
 *       {
 *         id: 2,
 *         content: "收到，马上处理",
 *         children: [
 *           { id: 3, content: "谢谢！" }
 *         ]
 *       }
 *     ]
 *   }
 * ]
 * }</pre>
 * </p>
 */
@Data
public class Comment {

    /**
     * 评论 ID（主键）
     */
    private Long id;

    /**
     * 需求 ID（外键）
     * <p>
     * 关联到 {@code demand} 表，标识此评论属于哪个需求
     * </p>
     */
    private Long demandId;

    /**
     * 评论人 ID
     * <p>
     * 关联到 {@code user} 表，记录谁发表了此评论
     * </p>
     */
    private Long userId;

    /**
     * 评论人姓名
     * <p>
     * 从 {@code user.real_name} 字段关联查询得到，用于前端展示
     * </p>
     */
    private String userName;

    /**
     * 评论人头像 URL
     * <p>
     * 从 {@code user.avatar} 字段关联查询得到，用于前端展示头像
     * </p>
     */
    private String userAvatar;

    /**
     * 评论内容
     * <p>
     * 支持纯文本或 Markdown 格式，最大长度由数据库定义
     * </p>
     */
    private String content;

    /**
     * 父评论 ID
     * <p>
     * 用于实现嵌套回复：
     * <ul>
     *   <li>NULL - 一级评论，直接评论需求</li>
     *   <li>非 NULL - 二级回复，指向某条评论的 ID</li>
     * </ul>
     * </p>
     */
    private Long parentId;

    /**
     * 创建时间
     * <p>
     * 评论发表的时间，精确到秒
     * </p>
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * <p>
     * 目前与创建时间相同，预留用于未来支持编辑功能
     * </p>
     */
    private LocalDateTime updateTime;
}
