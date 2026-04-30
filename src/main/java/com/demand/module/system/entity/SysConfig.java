package com.demand.module.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 */
@Data
public class SysConfig {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 配置键（唯一标识）
     */
    private String configKey;

    /**
     * 配置值（支持长文本）
     */
    private String configValue;

    /**
     * 值类型: string/number/boolean/json
     */
    private String configType;

    /**
     * 配置分组: system/file/email/sms/security/websocket
     */
    private String configGroup;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 是否系统内置: 1-是(不可删除) 0-否
     */
    private Integer isSystem;

    /**
     * 是否前端可见: 1-是 0-否(敏感配置)
     */
    private Integer isVisible;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
