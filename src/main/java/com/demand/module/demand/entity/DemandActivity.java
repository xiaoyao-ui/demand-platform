package com.demand.module.demand.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 需求动态时间轴实体类
 * <p>
 * 对应数据库 {@code demand_activity} 表，记录需求生命周期中的所有关键操作。
 * 形成完整的时间轴，用于审计追踪和故障排查。
 * </p>
 * 
 * <h3>典型动态类型（actionType）：</h3>
 * <ul>
 *   <li><b>CREATE</b> - 创建需求</li>
 *   <li><b>SUBMIT</b> - 提交审核</li>
 *   <li><b>APPROVE</b> - 审批需求（通过/拒绝）</li>
 *   <li><b>ASSIGN</b> - 分配负责人</li>
 *   <li><b>STATUS_CHANGE</b> - 状态变更</li>
 *   <li><b>ATTACHMENT</b> - 上传附件</li>
 *   <li><b>ATTACHMENT_DELETE</b> - 删除附件</li>
 *   <li><b>WITHDRAW</b> - 撤回需求</li>
 * </ul>
 * 
 * <h3>典型数据示例：</h3>
 * <pre>
 * +----+-----------+--------------+----------------+---------------+------------------------------------------+---------------------+
 * | id | demand_id | operator_id  | operator_name  | action_type   | content                                  | create_time         |
 * +----+-----------+--------------+----------------+---------------+------------------------------------------+---------------------+
 * |  1 |       100 |           10 | 张三           | CREATE        | 创建了需求                               | 2026-04-23 10:00:00 |
 * |  2 |       100 |           15 | 李四(经理)     | APPROVE       | 审批通过了需求                           | 2026-04-23 11:00:00 |
 * |  3 |       100 |           15 | 李四(经理)     | ASSIGN        | 分配给王五                               | 2026-04-23 11:30:00 |
 * |  4 |       100 |           20 | 王五           | STATUS_CHANGE | 将状态从【审批通过】修改为【开发中】      | 2026-04-23 14:00:00 |
 * +----+-----------+--------------+----------------+---------------+------------------------------------------+---------------------+
 * </pre>
 */
@Data
public class DemandActivity {

    /**
     * 动态 ID（主键）
     */
    private Long id;

    /**
     * 需求 ID（外键）
     * <p>
     * 关联到 {@code demand} 表，标识此动态属于哪个需求
     * </p>
     */
    private Long demandId;

    /**
     * 操作人 ID
     * <p>
     * 关联到 {@code user} 表，记录谁执行了此操作
     * </p>
     */
    private Long operatorId;

    /**
     * 操作人姓名
     * <p>
     * 冗余存储，避免每次查询都关联 {@code user} 表
     * </p>
     */
    private String operatorName;

    /**
     * 操作人头像 URL
     * <p>
     * 从 {@code user.avatar} 字段关联查询得到，用于前端展示头像
     * </p>
     */
    private String operatorAvatar;

    /**
     * 动作类型
     * <p>
     * 标识操作的类型，例如：CREATE、APPROVE、ASSIGN、STATUS_CHANGE 等
     * </p>
     */
    private String actionType;

    /**
     * 动态内容描述
     * <p>
     * 人类可读的操作描述，例如：
     * <ul>
     *   <li>"创建了需求"</li>
     *   <li>"审批通过了需求（意见：同意）"</li>
     *   <li>"将状态从【待审批】修改为【开发中】"</li>
     * </ul>
     * </p>
     */
    private String content;

    /**
     * 扩展数据（JSON 格式）
     * <p>
     * 存储额外的结构化数据，例如：
     * <ul>
     *   <li>状态变更时的旧状态和新状态</li>
     *   <li>附件上传时的文件名和大小</li>
     * </ul>
     * </p>
     */
    private String extraData;

    /**
     * 创建时间
     * <p>
     * 操作执行的时间，精确到秒
     * </p>
     */
    private LocalDateTime createTime;
}
