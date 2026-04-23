package com.demand.module.demand.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 需求导出 Excel 实体
 */
@Data
@HeadRowHeight(25) // 表头行高
@ContentRowHeight(20) // 内容行高
public class DemandExportDTO {

    @ExcelProperty(value = "需求ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "标题", index = 1)
    @ColumnWidth(40)
    private String title;

    @ExcelProperty(value = "类型", index = 2)
    @ColumnWidth(12)
    private String type;

    @ExcelProperty(value = "优先级", index = 3)
    @ColumnWidth(10)
    private String priority;

    @ExcelProperty(value = "状态", index = 4)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "提出人", index = 5)
    @ColumnWidth(12)
    private String proposerName;

    @ExcelProperty(value = "负责人", index = 6)
    @ColumnWidth(12)
    private String assigneeName;

    @ExcelProperty(value = "所属模块", index = 7)
    @ColumnWidth(15)
    private String module;

    @ExcelProperty(value = "创建时间", index = 8)
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ExcelProperty(value = "更新时间", index = 9)
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
