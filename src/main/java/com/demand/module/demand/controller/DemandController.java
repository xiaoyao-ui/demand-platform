package com.demand.module.demand.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.demand.common.PageResult;
import com.demand.common.Result;
import com.demand.config.RateLimit;
import com.demand.config.RequirePermission;
import com.demand.module.demand.dto.BatchAssignDTO;
import com.demand.module.demand.dto.DashboardStats;
import com.demand.module.demand.dto.DemandExportDTO;
import com.demand.module.demand.dto.*;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandActivity;
import com.demand.module.demand.entity.DemandStatusHistory;
import com.demand.module.demand.service.*;
import com.demand.module.dict.service.DictService;
import com.demand.module.user.service.PermissionService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 需求管理控制器
 * <p>
 * 提供需求的增删改查、审批、分配、导出等功能接口。
 * </p>
 */
@Tag(name = "需求管理", description = "需求的增删改查和状态管理接口")
@RestController
@RequestMapping("/api/demand")
@RequiredArgsConstructor
public class DemandController {

    private final DemandService demandService;
    private final DemandApprovalService approvalService;
    private final DemandAssignmentService assignmentService;
    private final PermissionService permissionService;
    private final DemandActivityService demandActivityService;
    private final DictService dictService;
    private final DemandDependencyService dependencyService;

    /**
     * 创建需求
     */
    @Operation(summary = "创建需求", description = "创建新的需求记录（只读用户不可用）")
    @PostMapping
    @RequirePermission(roles = {1, 2, 3, 4, 5, 6})
    public Result<?> createDemand(@Valid @RequestBody DemandCreateDTO createDTO) {
        Long currentUserId = permissionService.getCurrentUserId();
        demandService.createDemand(createDTO, currentUserId);
        return Result.success();
    }

    /**
     * 查询需求详情
     */
    @Operation(summary = "查询需求详情", description = "根据需求ID查询详细信息（所有角色可访问）")
    @GetMapping("/{id}")
    public Result<Demand> getDemandById(@Parameter(description = "需求ID") @PathVariable Long id) {
        Demand demand = demandService.getDemandById(id);
        return Result.success(demand);
    }

    /**
     * 更新需求
     */
    @Operation(summary = "更新需求", description = "更新需求信息和状态（提出人、产品经理、项目经理、超级管理员可操作）")
    @PutMapping("/{id}")
    @RequirePermission(roles = {1, 2, 3, 4}, requireOwner = true, resourceIdParam = "id")
    public Result<?> updateDemand(@Parameter(description = "需求ID") @PathVariable Long id,
                                  @Valid @RequestBody DemandCreateDTO updateDTO) {
        Long operatorId = permissionService.getCurrentUserId();

        Demand demand = new Demand();
        demand.setTitle(updateDTO.getTitle());
        demand.setDescription(updateDTO.getDescription());
        demand.setType(updateDTO.getType());
        demand.setPriority(updateDTO.getPriority());
        demand.setModuleId(updateDTO.getModuleId());
        demand.setExpectedEndDate(updateDTO.getExpectedEndDate());

        demandService.updateDemand(id, demand, operatorId);
        return Result.success();
    }

    /**
     * 删除需求
     */
    @Operation(summary = "删除需求", description = "删除指定需求（提出人或超级管理员可操作）")
    @DeleteMapping("/{id}")
    @RequirePermission(roles = {1, 2}, requireOwner = true, resourceIdParam = "id")
    public Result<?> deleteDemand(@Parameter(description = "需求ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        demandService.deleteDemand(id, currentUserId);
        return Result.success();
    }

    /**
     * 分页查询需求
     */
    @Operation(summary = "分页查询需求", description = "根据条件分页查询需求列表（所有角色可访问）")
    @PostMapping("/query")
    public Result<PageResult<Demand>> queryDemands(@RequestBody DemandQueryDTO queryDTO) {
        PageResult<Demand> pageResult = demandService.queryDemands(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 审批需求
     */
    @Operation(summary = "审批需求", description = "项目经理审批需求（仅项目经理和管理员）")
    @PostMapping("/approve")
    @RequirePermission(roles = {1, 4})
    public Result<?> approveDemand(@Valid @RequestBody DemandApproveDTO approveDTO) {
        Long approverId = permissionService.getCurrentUserId();
        approvalService.approveDemand(approveDTO, approverId);
        return Result.success();
    }

    /**
     * 分配需求
     */
    @Operation(summary = "分配需求", description = "分配需求给负责人（仅项目经理和管理员）")
    @PostMapping("/assign")
    @RequirePermission(roles = {1, 4})
    public Result<?> assignDemand(@Valid @RequestBody DemandAssignDTO assignDTO) {
        Long operatorId = permissionService.getCurrentUserId();
        assignmentService.assignDemand(assignDTO, operatorId);
        return Result.success();
    }

    /**
     * 查询待审批需求
     */
    @Operation(summary = "查询待审批需求", description = "查询所有待审批的需求（仅项目经理、产品经理和超级管理员）")
    @GetMapping("/pending-approval")
    @RequirePermission(roles = {1, 3, 4})
    public Result<PageResult<Demand>> getPendingApprovalDemands(DemandQueryDTO queryDTO) {
        queryDTO.setStatus("PENDING_REVIEW");
        PageResult<Demand> pageResult = demandService.queryDemands(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询状态历史
     */
    @Operation(summary = "查询状态历史", description = "查询需求的状态变更历史记录（所有角色可访问）")
    @GetMapping("/{id}/status-history")
    public Result<List<DemandStatusHistory>> getStatusHistory(@Parameter(description = "需求ID") @PathVariable Long id) {
        List<DemandStatusHistory> history = demandService.getStatusHistory(id);
        return Result.success(history);
    }

    /**
     * 获取仪表盘统计
     */
    @Operation(summary = "获取仪表盘统计", description = "获取需求统计、类型分布、趋势等数据")
    @GetMapping("/dashboard/stats")
    public Result<DashboardStats> getDashboardStats() {
        DashboardStats stats = demandService.getDashboardStats();
        return Result.success(stats);
    }

    /**
     * 提交需求审核
     */
    @Operation(summary = "提交需求审核", description = "将草稿状态的需求提交审核")
    @PostMapping("/{id}/submit")
    @RequirePermission(roles = {2, 3, 5, 6}, requireOwner = true, resourceIdParam = "id")
    public Result<?> submitDemand(@Parameter(description = "需求ID") @PathVariable Long id) {
        Long currentUserId = permissionService.getCurrentUserId();
        demandService.submitForApproval(id, currentUserId);
        return Result.success();
    }

    /**
     * 撤回需求
     */
    @Operation(summary = "撤回需求", description = "提出人撤回待审批的需求")
    @PostMapping("/{id}/withdraw")
    @RequirePermission(roles = {2, 3, 5, 6}, requireOwner = true, resourceIdParam = "id")
    public Result<?> withdrawDemand(@Parameter(description = "需求ID") @PathVariable Long id,
                                    @RequestBody(required = false) DemandWithdrawDTO withdrawDTO) {
        Long currentUserId = permissionService.getCurrentUserId();
        String reason = withdrawDTO != null ? withdrawDTO.getReason() : "提出人主动撤回";
        demandService.withdrawDemand(id, currentUserId, reason);
        return Result.success();
    }

    /**
     * 获取需求操作动态
     */
    @Operation(summary = "获取需求操作动态", description = "查看需求变更审计时间轴")
    @GetMapping("/{id}/activities")
    public Result<List<DemandActivity>> getActivities(@PathVariable Long id) {
        return Result.success(demandActivityService.getActivitiesByDemandId(id));
    }

    /**
     * 批量分配负责人
     */
    @Operation(summary = "批量分配负责人", description = "批量将需求分配给指定负责人")
    @PutMapping("/batch-assign")
    @RequirePermission(roles = {1, 4})
    public Result<?> batchAssign(@Valid @RequestBody BatchAssignDTO dto) {
        Long operatorId = permissionService.getCurrentUserId();
        demandService.batchAssign(dto.getIds(), dto.getAssigneeId(), operatorId);
        return Result.success("批量分配成功");
    }

    /**
     * 获取项目统计
     */
    @Operation(summary = "获取项目统计", description = "获取项目下的需求数量、完成率等统计数据")
    @GetMapping("/project/{projectId}/stats")
    public Result<com.demand.module.demand.dto.ProjectStatsDTO> getProjectStats(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        com.demand.module.demand.dto.ProjectStatsDTO stats = demandService.getProjectStats(projectId);
        return Result.success(stats);
    }

    /**
     * 获取迭代看板
     */
    @Operation(summary = "获取迭代看板", description = "获取迭代内的需求分布、进度追踪等数据")
    @GetMapping("/iteration/{iterationId}/kanban")
    public Result<com.demand.module.demand.dto.IterationKanbanDTO> getIterationKanban(
            @Parameter(description = "迭代ID") @PathVariable Long iterationId) {
        com.demand.module.demand.dto.IterationKanbanDTO kanban = demandService.getIterationKanban(iterationId);
        return Result.success(kanban);
    }

    /**
     * 获取项目看板
     */
    @Operation(summary = "获取项目看板", description = "获取项目的需求看板数据，按状态分组")
    @GetMapping("/project/{projectId}/kanban")
    public Result<com.demand.module.demand.dto.DemandKanbanDTO> getProjectKanban(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        com.demand.module.demand.dto.DemandKanbanDTO kanban = demandService.getProjectKanban(projectId);
        return Result.success(kanban);
    }

    /**
     * 获取项目工时统计
     */
    @Operation(summary = "获取项目工时统计", description = "获取项目的工时统计数据，包括总体统计、人员分布、类型分布")
    @GetMapping("/project/{projectId}/workload/stats")
    public Result<com.demand.module.demand.dto.WorkloadStatsDTO> getProjectWorkloadStats(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        com.demand.module.demand.dto.WorkloadStatsDTO stats = demandService.getProjectWorkloadStats(projectId);
        return Result.success(stats);
    }

    /**
     * 批量更新状态
     */
    @Operation(summary = "批量更新状态", description = "批量修改多个需求的状态")
    @PutMapping("/batch/status")
    @RequirePermission(roles = {1, 4})
    public Result<?> batchUpdateStatus(@Valid @RequestBody com.demand.module.demand.dto.BatchUpdateStatusDTO dto) {
        Long operatorId = permissionService.getCurrentUserId();
        demandService.batchUpdateStatus(dto.getIds(), dto.getStatus(), dto.getReason(), operatorId);
        return Result.success("批量更新状态成功");
    }

    /**
     * 批量添加标签
     */
    @Operation(summary = "批量添加标签", description = "为多个需求批量添加标签")
    @PostMapping("/batch/tags")
    @RequirePermission(roles = {1, 2, 3, 4}, requireOwner = true, resourceIdParam = "ids")
    public Result<?> batchAddTags(@Valid @RequestBody com.demand.module.demand.dto.BatchAddTagsDTO dto) {
        Long operatorId = permissionService.getCurrentUserId();
        demandService.batchAddTags(dto.getIds(), dto.getTagIds(), operatorId);
        return Result.success("批量添加标签成功");
    }

    /**
     * 批量移动需求
     */
    @Operation(summary = "批量移动需求", description = "将多个需求移动到其他项目/模块/迭代")
    @PutMapping("/batch/move")
    @RequirePermission(roles = {1, 4})
    public Result<?> batchMoveDemands(@Valid @RequestBody com.demand.module.demand.dto.BatchMoveDemandDTO dto) {
        Long operatorId = permissionService.getCurrentUserId();
        demandService.batchMoveDemands(dto.getIds(), dto.getTargetProjectId(), 
                dto.getTargetModuleId(), dto.getTargetIterationId(), operatorId);
        return Result.success("批量移动需求成功");
    }

    /**
     * 批量删除需求
     */
    @Operation(summary = "批量删除需求", description = "批量删除多个需求")
    @DeleteMapping("/batch")
    @RequirePermission(roles = {1, 2}, requireOwner = true, resourceIdParam = "ids")
    public Result<?> batchDeleteDemands(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        Long operatorId = permissionService.getCurrentUserId();
        demandService.batchDeleteDemands(ids, operatorId);
        return Result.success("批量删除需求成功");
    }

    /**
     * 分析需求变更影响
     */
    @Operation(summary = "分析变更影响", description = "基于依赖关系分析需求变更的影响范围")
    @GetMapping("/{id}/impact-analysis")
    public Result<com.demand.module.demand.dto.ImpactAnalysisDTO> analyzeImpact(
            @Parameter(description = "需求ID") @PathVariable Long id,
            @Parameter(description = "变更类型：STATUS, CONTENT, PRIORITY") 
            @RequestParam(defaultValue = "CONTENT") String changeType) {
        com.demand.module.demand.dto.ImpactAnalysisDTO analysis = 
                dependencyService.analyzeImpact(id, changeType);
        return Result.success(analysis);
    }

    /**
     * 导出需求列表 Excel
     */
    @Operation(summary = "导出需求列表 Excel", description = "根据查询条件导出需求数据为 Excel 文件")
    @RateLimit(timeWindow = 60, maxRequests = 3, keyPrefix = "export_limit")
    @GetMapping("/export/excel")
    public void exportExcel(DemandQueryDTO queryDTO, HttpServletResponse response) {
        try {
            // 查询所有数据（限制最大导出数量）
            queryDTO.setPageNum(1);
            queryDTO.setPageSize(10000);
            PageResult<Demand> pageResult = demandService.queryDemands(queryDTO);
            List<Demand> demands = pageResult.getList();

            // 转换为导出 DTO
            List<DemandExportDTO> exportList = demands.stream().map(demand -> {
                DemandExportDTO dto = new DemandExportDTO();
                dto.setId(demand.getId());
                dto.setTitle(demand.getTitle());
                
                // 使用字典模块进行状态转义
                dto.setType(dictService.getDictName("demand_type", demand.getType()));
                dto.setPriority(dictService.getDictName("priority", demand.getPriority()));
                dto.setStatus(dictService.getDictName("demand_status", demand.getStatus()));
                
                // 设置人员信息
                dto.setProposerName(demand.getCreatorName());
                dto.setAssigneeName(demand.getAssigneeName());
                
                // 设置模块名称
                dto.setModuleName(demand.getModuleName());
                
                // 设置时间
                dto.setCreateTime(demand.getCreateTime());
                dto.setUpdateTime(demand.getUpdateTime());
                
                return dto;
            }).collect(Collectors.toList());

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("需求列表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

            // ================= 核心样式配置 =================
            
            // 1. 表头样式 - 深蓝色主题
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

            // 2. 内容样式 - 默认居中
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

            // 3. 组合策略
            HorizontalCellStyleStrategy horizontalCellStyleStrategy = 
                    new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);

            // 4. 执行导出
            EasyExcel.write(response.getOutputStream(), DemandExportDTO.class)
                    .registerWriteHandler(horizontalCellStyleStrategy)
                    .registerWriteHandler(new PriorityAndStatusColorHandler())
                    .sheet("需求列表")
                    .doWrite(exportList);

        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 自定义单元格处理器 - 实现优先级和状态的条件格式着色
     */
    private static class PriorityAndStatusColorHandler implements com.alibaba.excel.write.handler.CellWriteHandler {
        
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

            int columnIndex = cell.getColumnIndex();
            org.apache.poi.ss.usermodel.Sheet sheet = cell.getSheet();
            org.apache.poi.ss.usermodel.Workbook workbook = sheet.getWorkbook();

            // 处理优先级列（第4列，index=3）
            if (columnIndex == 3) {
                String priority = cell.getStringCellValue();
                if (priority != null) {
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    style.cloneStyleFrom(cell.getCellStyle());
                    
                    short colorIndex;
                    short fontColor = IndexedColors.BLACK.getIndex();
                    
                    switch (priority) {
                        case "紧急":
                            colorIndex = IndexedColors.RED.getIndex();
                            fontColor = IndexedColors.WHITE.getIndex();
                            break;
                        case "高":
                            colorIndex = IndexedColors.ORANGE.getIndex();
                            break;
                        case "中":
                            colorIndex = IndexedColors.YELLOW.getIndex();
                            break;
                        case "低":
                            colorIndex = IndexedColors.LIGHT_GREEN.getIndex();
                            break;
                        default:
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

            // 处理状态列（第5列，index=4）
            if (columnIndex == 4) {
                String status = cell.getStringCellValue();
                if (status != null) {
                    org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                    style.cloneStyleFrom(cell.getCellStyle());
                    
                    short colorIndex;
                    
                    switch (status) {
                        case "已完成":
                            colorIndex = IndexedColors.BRIGHT_GREEN.getIndex();
                            break;
                        case "开发中":
                            colorIndex = IndexedColors.LIGHT_BLUE.getIndex();
                            break;
                        case "测试中":
                            colorIndex = IndexedColors.LIGHT_YELLOW.getIndex();
                            break;
                        case "已拒绝":
                            colorIndex = IndexedColors.GREY_40_PERCENT.getIndex();
                            break;
                        case "待审批":
                            colorIndex = IndexedColors.LEMON_CHIFFON.getIndex();
                            break;
                        case "审批通过":
                            colorIndex = IndexedColors.PALE_BLUE.getIndex();
                            break;
                        default:
                            return;
                    }
                    
                    style.setFillForegroundColor(colorIndex);
                    style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                    cell.setCellStyle(style);
                }
            }
        }
    }

    /**
     * 导出统计报表 PDF
     */
    @Operation(summary = "导出统计报表 PDF", description = "使用原生 API 绘制专业 PDF 报表")
    @RateLimit(timeWindow = 60, maxRequests = 3, keyPrefix = "export_limit")
    @GetMapping("/export/pdf")
    public void exportPdf(HttpServletResponse response) {
        try {
            DashboardStats stats = demandService.getDashboardStats();
            
            response.setContentType("application/pdf");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("需求统计报表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".pdf");

            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdf = new PdfDocument(writer);
            
            // 设置页面大小和边距
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36); // 上右下左

            // 1. 加载中文字体 - 跨平台兼容方案
            PdfFont chineseFont = loadChineseFont();
            final PdfFont font = chineseFont;

            // 2. 标题区域 - 添加分隔线
            Paragraph title = new Paragraph("需求管理统计报表")
                    .setFont(font).setFontSize(22).setBold()
                    .setFontColor(new DeviceRgb(41, 98, 255))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10) 
                    .setMarginBottom(5);
            document.add(title);
            
            // 添加装饰性分隔线
            Paragraph line = new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    .setFont(font).setFontSize(8)
                    .setFontColor(new DeviceRgb(200, 200, 200))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);
            document.add(line);
            
            // 3. 生成日期和导出人信息
            Paragraph date = new Paragraph("生成日期：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")))
                    .setFont(font).setFontSize(10).setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(20);
            document.add(date);

            // 4. 核心指标卡片 (4列布局)
            Table summaryTable = new Table(4).useAllAvailableWidth().setMarginBottom(25);
            summaryTable.addCell(createEnhancedSummaryCell(String.valueOf(stats.getTotalDemand()), "总需求数", font, new DeviceRgb(41, 98, 255)));
            summaryTable.addCell(createEnhancedSummaryCell(String.valueOf(stats.getPendingCount()), "待审批", font, new DeviceRgb(255, 152, 0)));
            summaryTable.addCell(createEnhancedSummaryCell(String.valueOf(stats.getDevelopingCount()), "开发中", font, new DeviceRgb(33, 150, 243)));
            summaryTable.addCell(createEnhancedSummaryCell(String.valueOf(stats.getCompletedCount()), "已完成", font, new DeviceRgb(76, 175, 80)));
            document.add(summaryTable);

            long total = stats.getTotalDemand();

            // 5. 类型分布表格
            document.add(createSectionTitleWithIcon("■ 需求类型分布", font, new DeviceRgb(41, 98, 255)));
            Table typeTable = new Table(new float[]{3, 1, 2}).useAllAvailableWidth();
            typeTable.addHeaderCell(createEnhancedHeaderCell("类型名称", font));
            typeTable.addHeaderCell(createEnhancedHeaderCell("数量", font));
            typeTable.addHeaderCell(createEnhancedHeaderCell("占比", font));
            
            String[] typeKeys = {"功能需求", "优化需求", "Bug修复"};
            
            for (String key : typeKeys) {
                Long count = stats.getTypeDistribution().getOrDefault(key, 0L);
                double percent = total > 0 ? (count * 100.0 / total) : 0;
                
                typeTable.addCell(createContentCell(key, font));
                typeTable.addCell(createContentCell(String.valueOf(count), font));
                typeTable.addCell(createContentCell(String.format("%.1f%%", percent), font));
            }
            document.add(typeTable);
            document.add(new Paragraph("\n"));

            // 6. 优先级分布表格
            document.add(createSectionTitleWithIcon("■ 优先级统计", font, new DeviceRgb(255, 87, 34)));
            Table priorityTable = new Table(new float[]{2, 1, 2}).useAllAvailableWidth();
            priorityTable.addHeaderCell(createEnhancedHeaderCell("优先级", font));
            priorityTable.addHeaderCell(createEnhancedHeaderCell("数量", font));
            priorityTable.addHeaderCell(createEnhancedHeaderCell("占比", font));
            
            String[] priorityKeys = {"紧急", "高", "中", "低"};
            DeviceRgb[] priorityColors = {
                new DeviceRgb(244, 67, 54),
                new DeviceRgb(255, 152, 0),
                new DeviceRgb(255, 235, 59),
                new DeviceRgb(76, 175, 80)
            };
            
            for (int i = 0; i < priorityKeys.length; i++) {
                String key = priorityKeys[i];
                Long count = stats.getPriorityDistribution().getOrDefault(key, 0L);
                double percent = total > 0 ? (count * 100.0 / total) : 0;
                
                priorityTable.addCell(createColoredContentCell(key, priorityColors[i], font));
                priorityTable.addCell(createContentCell(String.valueOf(count), font));
                priorityTable.addCell(createContentCell(String.format("%.1f%%", percent), font));
            }
            document.add(priorityTable);
            document.add(new Paragraph("\n"));

            // 7. 页脚
            Paragraph footer = new Paragraph("— 由需求管理平台自动生成 —")
                    .setFont(font).setFontSize(9).setFontColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(20);
            document.add(footer);
            
            Paragraph timestamp = new Paragraph("Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(font).setFontSize(7).setFontColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5);
            document.add(timestamp);

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("导出 PDF 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 跨平台加载中文字体
     */
    private PdfFont loadChineseFont() {
        String[] fontPaths = {
            "C:/Windows/Fonts/msyh.ttf",
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/chinese/simsun.ttc",
            "/System/Library/Fonts/PingFang.ttc,0",
            "/Library/Fonts/Arial Unicode.ttf"
        };
        
        for (String path : fontPaths) {
            try {
                return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H);
            } catch (Exception ignored) {
            }
        }
        
        try {
            return PdfFontFactory.createFont(StandardFonts.HELVETICA);
        } catch (Exception e) {
            throw new RuntimeException("无法加载任何字体", e);
        }
    }

    /**
     * 创建增强版核心指标卡片
     */
    private Cell createEnhancedSummaryCell(String value, String label, PdfFont font, DeviceRgb color) {
        Cell cell = new Cell();
        cell.setBorder(new SolidBorder(color, 1.5f));
        cell.setPadding(12);
        
        // 计算浅色背景（原颜色的10%透明度效果）
        float[] rgbValues = color.getColorValue();
        float bgRed = rgbValues[0] * 0.1f + 0.9f;
        float bgGreen = rgbValues[1] * 0.1f + 0.9f;
        float bgBlue = rgbValues[2] * 0.1f + 0.9f;
        cell.setBackgroundColor(new DeviceRgb(bgRed, bgGreen, bgBlue));
        
        Paragraph valuePara = new Paragraph(value)
                .setFont(font).setFontSize(26).setBold()
                .setFontColor(color)
                .setTextAlignment(TextAlignment.CENTER);
        cell.add(valuePara);
        
        Paragraph labelPara = new Paragraph(label)
                .setFont(font).setFontSize(10)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(3);
        cell.add(labelPara);
        
        return cell;
    }

    /**
     * 创建带图标的章节标题
     */
    private Paragraph createSectionTitleWithIcon(String text, PdfFont font, DeviceRgb color) {
        return new Paragraph(text)
                .setFont(font).setFontSize(14).setBold()
                .setFontColor(color)
                .setMarginTop(20).setMarginBottom(8);
    }

    /**
     * 创建增强版表头单元格
     */
    private Cell createEnhancedHeaderCell(String text, PdfFont font) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text)
                .setFont(font).setFontSize(11).setBold()
                .setFontColor(ColorConstants.WHITE));
        cell.setBackgroundColor(new DeviceRgb(41, 98, 255));
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
        cell.setPadding(10);
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 0.5f));
        return cell;
    }

    /**
     * 创建普通内容单元格
     */
    private Cell createContentCell(String text, PdfFont font) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text)
                .setFont(font).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));
        cell.setPadding(6);
        cell.setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
        return cell;
    }

    /**
     * 创建进度条单元格
     */
    private Cell createProgressBarCell(double percent, DeviceRgb color, PdfFont font) {
        Cell cell = new Cell();
        cell.setBorder(Border.NO_BORDER);
        cell.setPadding(5);
        
        float barWidth = (float) (percent / 100 * 80);
        
        if (barWidth > 0) {
            Paragraph bar = new Paragraph("█")
                    .setFont(font).setFontSize(8)
                    .setFontColor(color)
                    .setCharacterSpacing(barWidth - 2);
            cell.add(bar);
        }
        
        Paragraph percentText = new Paragraph(String.format("%.1f%%", percent))
                .setFont(font).setFontSize(8)
                .setFontColor(ColorConstants.DARK_GRAY);
        cell.add(percentText);
        
        cell.setTextAlignment(TextAlignment.LEFT);
        return cell;
    }

    /**
     * 创建带颜色的内容单元格
     */
    private Cell createColoredContentCell(String text, DeviceRgb bgColor, PdfFont font) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text)
                .setFont(font).setFontSize(10)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
        return cell;
    }

}
