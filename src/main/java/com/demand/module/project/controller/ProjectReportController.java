package com.demand.module.project.controller;

import com.alibaba.excel.EasyExcel;
import com.demand.common.Result;
import com.demand.module.project.dto.ProjectReportDTO;
import com.demand.module.project.service.ProjectReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目报告控制器
 */
@Slf4j
@Tag(name = "项目报告", description = "项目周报、月报生成和导出")
@RestController
@RequestMapping("/api/project/{projectId}/report")
@RequiredArgsConstructor
public class ProjectReportController {

    private final ProjectReportService reportService;

    /**
     * 获取项目周报
     */
    @Operation(summary = "获取周报", description = "生成项目本周的工作报告")
    @GetMapping("/weekly")
    public Result<ProjectReportDTO> getWeeklyReport(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        ProjectReportDTO report = reportService.generateWeeklyReport(projectId);
        return Result.success(report);
    }

    /**
     * 获取项目月报
     */
    @Operation(summary = "获取月报", description = "生成项目本月的工作报告")
    @GetMapping("/monthly")
    public Result<ProjectReportDTO> getMonthlyReport(
            @Parameter(description = "项目ID") @PathVariable Long projectId) {
        ProjectReportDTO report = reportService.generateMonthlyReport(projectId);
        return Result.success(report);
    }

    /**
     * 导出周报为 Excel
     */
    @Operation(summary = "导出周报Excel", description = "将项目周报导出为 Excel 文件")
    @GetMapping("/weekly/export/excel")
    public void exportWeeklyExcel(@Parameter(description = "项目ID") @PathVariable Long projectId,
                                  HttpServletResponse response) {
        try {
            ProjectReportDTO report = reportService.generateWeeklyReport(projectId);
            exportReportToExcel(report, "周报", response);
        } catch (Exception e) {
            log.error("导出周报Excel失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出月报为 Excel
     */
    @Operation(summary = "导出月报Excel", description = "将项目月报导出为 Excel 文件")
    @GetMapping("/monthly/export/excel")
    public void exportMonthlyExcel(@Parameter(description = "项目ID") @PathVariable Long projectId,
                                   HttpServletResponse response) {
        try {
            ProjectReportDTO report = reportService.generateMonthlyReport(projectId);
            exportReportToExcel(report, "月报", response);
        } catch (Exception e) {
            log.error("导出月报Excel失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出报告为 Excel
     */
    private void exportReportToExcel(ProjectReportDTO report, String reportType,
                                     HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        String fileName = URLEncoder.encode(
                String.format("%s_%s_%s至%s",
                        report.getProjectName(), reportType,
                        report.getStartDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                        report.getEndDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))),
                StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

        // 构建导出数据
        List<ReportExportDTO> exportData = buildExportData(report);

        EasyExcel.write(response.getOutputStream(), ReportExportDTO.class)
                .sheet(reportType)
                .doWrite(exportData);
    }

    /**
     * 构建导出数据
     */
    private List<ReportExportDTO> buildExportData(ProjectReportDTO report) {
        List<ReportExportDTO> exportData = new ArrayList<>();

        // 1. 添加总体统计
        ReportExportDTO summaryRow = new ReportExportDTO();
        summaryRow.setCategory("总体统计");
        summaryRow.setItem("需求总数");
        summaryRow.setValue(String.valueOf(report.getSummary().getTotalDemands()));
        exportData.add(summaryRow);

        summaryRow = new ReportExportDTO();
        summaryRow.setCategory("总体统计");
        summaryRow.setItem("完成率");
        summaryRow.setValue(report.getSummary().getCompletionRate() + "%");
        exportData.add(summaryRow);

        summaryRow = new ReportExportDTO();
        summaryRow.setCategory("总体统计");
        summaryRow.setItem("总工时");
        summaryRow.setValue(report.getSummary().getTotalHours() + "小时");
        exportData.add(summaryRow);

        // 2. 添加完成的需求
        exportData.add(createSectionHeader("本周/本月完成的需求"));
        for (ProjectReportDTO.ReportDemand demand : report.getCompletedDemands()) {
            ReportExportDTO row = new ReportExportDTO();
            row.setCategory("完成需求");
            row.setItem(demand.getTitle());
            row.setValue(demand.getAssigneeName() != null ?
                    "负责人：" + demand.getAssigneeName() : "未分配");
            exportData.add(row);
        }

        // 3. 添加成员工作量
        exportData.add(createSectionHeader("成员工作量"));
        for (ProjectReportDTO.MemberWorkload workload : report.getMemberWorkloads()) {
            ReportExportDTO row = new ReportExportDTO();
            row.setCategory("成员工作量");
            row.setItem(workload.getUserName());
            row.setValue(String.format("需求：%d个，工时：%.1f小时",
                    workload.getDemandCount(), workload.getActualHours()));
            exportData.add(row);
        }

        return exportData;
    }

    /**
     * 创建分节标题
     */
    private ReportExportDTO createSectionHeader(String title) {
        ReportExportDTO row = new ReportExportDTO();
        row.setCategory("");
        row.setItem("【" + title + "】");
        row.setValue("");
        return row;
    }

    /**
     * 报告导出 DTO
     */
    @lombok.Data
    @com.alibaba.excel.annotation.ExcelIgnoreUnannotated
    public static class ReportExportDTO {
        @com.alibaba.excel.annotation.ExcelProperty(value = "类别", index = 0)
        private String category;

        @com.alibaba.excel.annotation.ExcelProperty(value = "项目", index = 1)
        private String item;

        @com.alibaba.excel.annotation.ExcelProperty(value = "数值/说明", index = 2)
        private String value;
    }
}
