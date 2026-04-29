package com.demand.module.demand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 需求质量评分实体类
 * <p>
 * 记录需求的完整性评分，帮助团队识别低质量需求。
 * </p>
 */
@Data
@TableName("demand_quality_score")
public class DemandQualityScore {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 需求 ID
     */
    private Long demandId;

    /**
     * 总分（0-100）
     */
    private Integer totalScore;

    /**
     * 评级：EXCELLENT-优秀(90-100), GOOD-良好(75-89), FAIR-一般(60-74), POOR-较差(<60)
     */
    private String rating;

    /**
     * 必填字段得分（0-30）
     */
    private Integer requiredFieldsScore;

    /**
     * 描述质量得分（0-25）
     */
    private Integer descriptionScore;

    /**
     * 验收标准得分（0-20）
     */
    private Integer acceptanceCriteriaScore;

    /**
     * 技术细节得分（0-15）
     */
    private Integer technicalDetailsScore;

    /**
     * 业务价值得分（0-10）
     */
    private Integer businessValueScore;

    /**
     * 评分详情（JSON 格式，记录扣分项）
     */
    private String scoreDetails;

    /**
     * 建议改进项（JSON 数组）
     */
    private String suggestions;

    /**
     * 评分时间
     */
    private LocalDateTime scoreTime;

    /**
     * 是否已提醒（低质量需求）
     */
    private Boolean notified;
}
