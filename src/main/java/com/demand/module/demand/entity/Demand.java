package com.demand.module.demand.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 需求实体类
 * <p>
 * 对应数据库 {@code demand} 表，存储系统中的所有需求信息。
 * 支持完整的生命周期管理：草稿 → 待审批 → 审批通过 → 开发中 → 测试中 → 已完成。
 * </p>
 * 
 * <h3>需求状态流转：</h3>
 * <pre>
 * 草稿(6) → 待审批(0) → 审批通过(1) → 开发中(2) → 测试中(3) → 已完成(4)
 *                    ↓
 *                  已拒绝(5) → 重新提交 → 待审批(0)
 * </pre>
 * 
 * <h3>典型数据示例：</h3>
 * <pre>
 * +----+------------------+------------+--------+----------+--------------+-------------+--------------+---------------------+---------------------+
 * | id | title            | type       | priority| status   | proposer_id | assignee_id | approver_id  | module          | expected_date       | actual_date         |
 * +----+------------------+------------+--------+----------+--------------+-------------+--------------+---------------------+---------------------+
 * |  1 | 用户登录功能      | 0          | 2      | 2        |           10 |          20 |           15 | 用户模块          | 2026-05-01 00:00:00 | NULL                |
 * |  2 | 性能优化-首页加载  | 1          | 1      | 4        |           12 |          22 |           15 | 前端模块          | 2026-04-20 00:00:00 | 2026-04-18 15:30:00 |
 * +----+------------------+------------+--------+----------+--------------+-------------+--------------+---------------------+---------------------+
 * </pre>
 */
@Data
public class Demand {

    /**
     * 需求 ID（主键）
     */
    private Long id;

    /**
     * 需求标题
     * <p>
     * 简短概括需求内容，例如："用户登录功能"、"首页性能优化"
     * </p>
     */
    private String title;

    /**
     * 需求描述
     * <p>
     * 详细描述需求的功能、背景、验收标准等。
     * 支持 Markdown 格式，可包含图片、列表、代码块等。
     * </p>
     */
    private String description;

    /**
     * 需求类型
     * <ul>
     *   <li>0 - 功能需求</li>
     *   <li>1 - 优化需求</li>
     *   <li>2 - Bug修复</li>
     * </ul>
     */
    private Integer type;

    /**
     * 需求优先级
     * <ul>
     *   <li>0 - 低</li>
     *   <li>1 - 中</li>
     *   <li>2 - 高</li>
     *   <li>3 - 紧急</li>
     * </ul>
     */
    private Integer priority;

    /**
     * 需求状态
     * <ul>
     *   <li>0 - 待审批</li>
     *   <li>1 - 审批通过</li>
     *   <li>2 - 开发中</li>
     *   <li>3 - 测试中</li>
     *   <li>4 - 已完成</li>
     *   <li>5 - 已拒绝</li>
     *   <li>6 - 草稿</li>
     * </ul>
     */
    private Integer status;

    /**
     * 需求提出人 ID
     * <p>
     * 关联到 {@code user} 表，记录谁创建了这个需求
     * </p>
     */
    private Long proposerId;

    /**
     * 需求负责人 ID
     * <p>
     * 关联到 {@code user} 表，记录谁负责开发这个需求。
     * 在需求分配时设置。
     * </p>
     */
    private Long assigneeId;

    /**
     * 审批人 ID（项目经理）
     * <p>
     * 关联到 {@code user} 表，记录谁审批了这个需求
     * </p>
     */
    private Long approverId;

    /**
     * 审批时间
     * <p>
     * 项目经理执行审批操作的时间
     * </p>
     */
    private LocalDateTime approveTime;

    /**
     * 审批意见
     * <p>
     * 项目经理填写的审批备注，选填。
     * 例如："需求描述不够清晰，请补充详细流程图"
     * </p>
     */
    private String approveComment;

    /**
     * 所属模块
     * <p>
     * 标识需求属于哪个功能模块，例如："用户模块"、"订单模块"、"支付模块"
     * </p>
     */
    private String module;

    /**
     * 期望完成时间
     * <p>
     * 提出人希望需求完成的日期，用于排期参考
     * </p>
     */
    private LocalDateTime expectedDate;

    /**
     * 实际完成时间
     * <p>
     * 需求状态变为"已完成"时自动记录，用于统计开发效率
     * </p>
     */
    private LocalDateTime actualDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    private LocalDateTime updateTime;

    // ==================== 以下字段为关联查询结果，非数据库字段 ====================

    /**
     * 提出人姓名
     * <p>
     * 通过 LEFT JOIN {@code user} 表获取，用于前端展示
     * </p>
     */
    private String proposerName;

    /**
     * 负责人姓名
     * <p>
     * 通过 LEFT JOIN {@code user} 表获取，用于前端展示
     * </p>
     */
    private String assigneeName;

    /**
     * 审批人姓名
     * <p>
     * 通过 LEFT JOIN {@code user} 表获取，用于前端展示
     * </p>
     */
    private String approverName;
}
