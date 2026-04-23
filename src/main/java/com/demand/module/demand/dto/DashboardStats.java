package com.demand.module.demand.dto;

import lombok.Data;
import java.util.Map;

/**
 * 仪表盘统计数据
 */
@Data
public class DashboardStats {
    // 总需求数
    private Long totalDemand;
    // 待审批数
    private Long pendingCount;
    // 开发中数
    private Long developingCount;
    // 已完成数
    private Long completedCount;
    // 类型分布
    private Map<String, Long> typeDistribution;
    // 优先级分布
    private Map<String, Long> priorityDistribution;
    // 近7天趋势（日期 -> 数量）
    private Map<String, Long> trendData;
}
