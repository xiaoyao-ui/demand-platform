package com.demand.module.module.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块实体类
 * <p>
 * 对应数据库 {@code module} 表，存储系统功能模块的配置信息。
 * 模块用于标识和管理不同的功能区域，支持动态启用/禁用。
 * </p>
 * 
 * <h3>典型模块示例：</h3>
 * <pre>
 * +----+--------------+----------+------------------+------+--------+
 * | id | name         | code     | description      | sort | status |
 * +----+--------------+----------+------------------+------+--------+
 * |  1 | 需求管理     | demand   | 需求的创建和审批  |    1 |      1 |
 * |  2 | 用户管理     | user     | 用户和权限管理    |    2 |      1 |
 * |  3 | 统计分析     | stats    | 数据统计和报表    |    3 |      0 |
 * +----+--------------+----------+------------------+------+--------+
 * </pre>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>前端动态菜单：根据启用的模块生成导航栏</li>
 *   <li>权限控制：限制某些角色只能访问特定模块</li>
 *   <li>功能开关：通过禁用模块临时关闭某项功能</li>
 * </ul>
 */
@Data
public class Module {

    /**
     * 模块 ID（主键）
     */
    private Long id;

    /**
     * 模块名称
     * <p>
     * 用于前端展示的中文名称，例如："需求管理"、"用户管理"
     * </p>
     */
    private String name;

    /**
     * 模块编码
     * <p>
     * 唯一标识符，用于后端逻辑判断和权限校验。
     * 例如：{@code demand}、{@code user}、{@code stats}
     * </p>
     * 
     * <p>
     * <b>命名规范</b>：
     * <ul>
     *   <li>使用小写字母和下划线</li>
     *   <li>长度不超过 50 个字符</li>
     *   <li>全局唯一，不能重复</li>
     * </ul>
     * </p>
     */
    private String code;

    /**
     * 模块描述
     * <p>
     * 用于说明模块的功能和用途，方便管理员理解
     * </p>
     */
    private String description;

    /**
     * 排序号
     * <p>
     * 数值越小越靠前，用于控制模块在前端菜单中的显示顺序
     * </p>
     */
    private Integer sort;

    /**
     * 模块状态
     * <ul>
     *   <li>0 - 禁用（不显示在前端）</li>
     *   <li>1 - 启用</li>
     * </ul>
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
