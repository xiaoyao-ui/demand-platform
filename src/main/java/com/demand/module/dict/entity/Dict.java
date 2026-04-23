package com.demand.module.dict.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据字典实体类
 * <p>
 * 对应数据库 {@code dict} 表，存储系统中的枚举值和配置项。
 * 通过类型（type）分组管理不同的字典集合。
 * </p>
 * 
 * <h3>典型字典类型：</h3>
 * <ul>
 *   <li><b>demand_type</b>：需求类型（0-功能需求，1-性能优化，2-Bug修复）</li>
 *   <li><b>priority</b>：优先级（0-低，1-中，2-高，3-紧急）</li>
 *   <li><b>demand_status</b>：需求状态（0-待审批，1-审批通过，2-开发中...）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * +----+--------------+------+--------+------+--------+--------+
 * | id | type         | code | name   | sort | status | remark |
 * +----+--------------+------+--------+------+--------+--------+
 * |  1 | demand_type  |    0 | 功能需求 |    1 |      1 |        |
 * |  2 | demand_type  |    1 | 性能优化 |    2 |      1 |        |
 * |  3 | priority     |    0 | 低      |    1 |      1 |        |
 * |  4 | priority     |    1 | 中      |    2 |      1 |        |
 * +----+--------------+------+--------+------+--------+--------+
 * </pre>
 */
@Data
public class Dict {

    /**
     * 字典 ID（主键）
     */
    private Long id;

    /**
     * 字典类型
     * <p>
     * 用于分组管理不同的字典集合，如 demand_type、priority、demand_status
     * </p>
     */
    private String type;

    /**
     * 字典编码
     * <p>
     * 实际业务中使用的值，例如：
     * <ul>
     *   <li>需求类型：0、1、2</li>
     *   <li>优先级：0、1、2、3</li>
     * </ul>
     * </p>
     */
    private Integer code;

    /**
     * 字典名称
     * <p>
     * 用于前端展示的中文描述，例如："功能需求"、"高优先级"
     * </p>
     */
    private String name;

    /**
     * 排序号
     * <p>
     * 数值越小越靠前，用于控制字典项在下拉框中的显示顺序
     * </p>
     */
    private Integer sort;

    /**
     * 状态
     * <ul>
     *   <li>0 - 禁用（不显示在前端）</li>
     *   <li>1 - 启用</li>
     * </ul>
     */
    private Integer status;

    /**
     * 备注说明
     * <p>
     * 用于记录字典项的详细说明或使用注意事项
     * </p>
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
