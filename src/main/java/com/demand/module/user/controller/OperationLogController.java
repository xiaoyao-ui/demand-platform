package com.demand.module.user.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.demand.common.PageResult;
import com.demand.common.Result;
import com.demand.config.RateLimit;
import com.demand.module.user.dto.OperationLogExportDTO;
import com.demand.module.user.entity.OperationLog;
import com.demand.module.user.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志控制器
 * <p>
 * 提供操作日志的分页查询和 Excel 导出功能。
 * 支持按用户名、操作描述、请求路径、状态、时间范围等多条件筛选。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>分页查询</b>：支持多条件组合筛选，返回分页结果</li>
 *   <li><b>Excel 导出</b>：根据筛选条件导出日志，带样式和颜色标记</li>
 *   <li><b>状态着色</b>：成功记录显示绿色背景，失败记录显示红色背景</li>
 * </ul>
 * 
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>审计追踪：查看谁在什么时间执行了什么操作</li>
 *   <li>故障排查：定位异常操作的详细信息（IP、参数、错误消息）</li>
 *   <li>合规要求：满足数据安全法规的日志留存要求</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志查询与导出接口")
public class OperationLogController {

    /**
     * 操作日志业务逻辑服务
     */
    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     * <p>
     * 支持以下筛选条件：
     * <ul>
     *   <li>{@code username}：操作用户名（模糊匹配）</li>
     *   <li>{@code uri}：请求路径（模糊匹配）</li>
     *   <li>{@code operation}：操作描述（模糊匹配）</li>
     *   <li>{@code status}：响应状态码（200-成功，其他-失败）</li>
     *   <li>{@code startTime/endTime}：时间范围筛选</li>
     * </ul>
     * </p>
     *
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页条数
     * @param username  用户名（可选）
     * @param uri       请求路径（可选）
     * @param operation 操作描述（可选）
     * @param status    响应状态码（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 分页结果（包含总记录数和当前页数据）
     */
    @Operation(summary = "查询操作日志列表", description = "分页查询操作日志，支持多条件筛选")
    @GetMapping("/list")
    public Result<PageResult<OperationLog>> getLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String uri,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        PageResult<OperationLog> pageResult = operationLogService.queryLogs(username, uri, operation, status, startTime, endTime, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 导出操作日志为 Excel 文件
     * <p>
     * 根据筛选条件导出所有匹配的日志记录（不分页），并应用以下样式：
     * <ul>
     *   <li><b>表头</b>：灰色背景 + 白色粗体文字 + 边框</li>
     *   <li><b>内容</b>：居中对齐 + 边框</li>
     *   <li><b>状态列</b>：成功-绿色背景，失败-红色背景 + 白色文字</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>限流保护</b>：60 秒内最多导出 3 次，防止频繁查询数据库
     * </p>
     * 
     * <p>
     * <b>文件名格式</b>：{@code 操作日志_20260423.xlsx}
     * </p>
     *
     * @param response  HTTP 响应对象（用于写入 Excel 数据流）
     * @param username  用户名（可选）
     * @param uri       请求路径（可选）
     * @param operation 操作描述（可选）
     * @param status    响应状态码（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     */
    @Operation(summary = "导出操作日志 Excel", description = "根据筛选条件导出操作日志为 Excel 文件")
    @RateLimit(timeWindow = 60, maxRequests = 3, keyPrefix = "export_limit")
    @GetMapping("/export/excel")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String uri,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            // 1. 查询所有符合条件的数据（不分页）
            List<OperationLog> logs = operationLogService.queryAllLogs(username, uri, operation, status, startTime, endTime);

            // 2. 转换为导出 DTO（简化字段，格式化状态文本）
            List<OperationLogExportDTO> exportList = logs.stream().map(log -> {
                OperationLogExportDTO dto = new OperationLogExportDTO();
                dto.setId(log.getId());
                dto.setUsername(log.getUsername());
                dto.setOperation(log.getOperation());
                dto.setMethod(log.getMethod());
                dto.setUri(log.getUri());
                dto.setIp(log.getIp());
                dto.setStatus(log.getStatus() == 200 ? "成功" : "失败");
                dto.setCreateTime(log.getCreateTime());
                return dto;
            }).collect(Collectors.toList());

            // 3. 设置响应头（触发浏览器下载）
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("操作日志_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

            // 4. 配置 Excel 样式
            // 表头样式：灰色背景 + 白色粗体
            WriteCellStyle headWriteCellStyle = new WriteCellStyle();
            headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            WriteFont headWriteFont = new WriteFont();
            headWriteFont.setFontHeightInPoints((short) 12);
            headWriteFont.setBold(true);
            headWriteFont.setColor(IndexedColors.WHITE.getIndex());
            headWriteCellStyle.setWriteFont(headWriteFont);
            headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headWriteCellStyle.setBorderLeft(BorderStyle.THIN);
            headWriteCellStyle.setBorderRight(BorderStyle.THIN);
            headWriteCellStyle.setBorderTop(BorderStyle.THIN);
            headWriteCellStyle.setBorderBottom(BorderStyle.THIN);

            // 内容样式：居中对齐 + 边框
            WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
            contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
            contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
            contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
            contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
            WriteFont contentWriteFont = new WriteFont();
            contentWriteFont.setFontHeightInPoints((short) 10);
            contentWriteCellStyle.setWriteFont(contentWriteFont);

            HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);

            // 5. 导出 Excel（应用样式和状态颜色处理器）
            EasyExcel.write(response.getOutputStream(), OperationLogExportDTO.class)
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(new StatusColorHandler())
                    .sheet("操作日志")
                    .doWrite(exportList);

        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 状态列颜色处理器
     * <p>
     * 根据状态文本（"成功"/"失败"）动态设置单元格背景色：
     * <ul>
     *   <li>成功：浅绿色背景 + 黑色文字</li>
     *   <li>失败：红色背景 + 白色文字</li>
     * </ul>
     * </p>
     */
    private static class StatusColorHandler implements com.alibaba.excel.write.handler.CellWriteHandler {
        
        @Override
        public void afterCellDispose(com.alibaba.excel.write.metadata.holder.WriteSheetHolder writeSheetHolder,
                                    com.alibaba.excel.write.metadata.holder.WriteTableHolder writeTableHolder,
                                    java.util.List<com.alibaba.excel.metadata.data.WriteCellData<?>> cellDataList,
                                    org.apache.poi.ss.usermodel.Cell cell,
                                    com.alibaba.excel.metadata.Head head,
                                    Integer relativeRowIndex,
                                    Boolean isHead) {
            if (isHead != null && isHead) {
                return;
            }

            // 处理状态列（第7列，index=6）
            if (cell.getColumnIndex() == 6) {
                String status = cell.getStringCellValue();
                if (status != null) {
                    org.apache.poi.ss.usermodel.Sheet sheet = cell.getSheet();
                    org.apache.poi.ss.usermodel.Workbook workbook = sheet.getWorkbook();
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    style.cloneStyleFrom(cell.getCellStyle());
                    
                    short colorIndex;
                    short fontColor = IndexedColors.BLACK.getIndex();
                    
                    if ("成功".equals(status)) {
                        colorIndex = IndexedColors.LIGHT_GREEN.getIndex();
                    } else if ("失败".equals(status)) {
                        colorIndex = IndexedColors.RED.getIndex();
                        fontColor = IndexedColors.WHITE.getIndex();
                    } else {
                        return;
                    }
                    
                    style.setFillForegroundColor(colorIndex);
                    style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                    
                    org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                    font.setColor(fontColor);
                    font.setFontHeightInPoints((short) 10);
                    style.setFont(font);
                    
                    cell.setCellStyle(style);
                }
            }
        }
    }
}
