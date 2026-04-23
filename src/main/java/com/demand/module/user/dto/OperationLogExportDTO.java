package com.demand.module.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志 Excel 导出 DTO
 * <p>
 * 使用 EasyExcel 注解定义列名、宽度和顺序。
 * </p>
 */
@Data
@HeadRowHeight(25)
@ContentRowHeight(20)
public class OperationLogExportDTO {

    @ExcelProperty(value = "日志ID", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "操作用户", index = 1)
    @ColumnWidth(15)
    private String username;

    @ExcelProperty(value = "操作描述", index = 2)
    @ColumnWidth(30)
    private String operation;

    @ExcelProperty(value = "请求方法", index = 3)
    @ColumnWidth(15)
    private String method;

    @ExcelProperty(value = "请求路径", index = 4)
    @ColumnWidth(35)
    private String uri;

    @ExcelProperty(value = "IP地址", index = 5)
    @ColumnWidth(18)
    private String ip;

    @ExcelProperty(value = "状态", index = 6)
    @ColumnWidth(10)
    private String status;

    @ExcelProperty(value = "操作时间", index = 7)
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
