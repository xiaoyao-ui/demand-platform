package com.demand.module.demand.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 需求趋势数据
 */
@Data
public class DemandTrend {
    // 日期
    private String date;
    // 数量
    private Long count;
}
