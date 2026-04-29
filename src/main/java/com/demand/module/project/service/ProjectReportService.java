package com.demand.module.project.service;

import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.project.dto.ProjectReportDTO;
import com.demand.module.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目报告服务
 * <p>
 * 自动生成项目周报和月报，支持 PDF/Excel 导出。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectReportService {

    private final ProjectMapper projectMapper;
    private final DemandMapper demandMapper;

    /**
     * 生成项目周报
     *
     * @param projectId 项目ID
     * @return 周报数据
     */
    public ProjectReportDTO generateWeeklyReport(Long projectId) {
        log.info("生成项目周报: projectId={}", projectId);

        // 计算本周的起止日期（周一到周日）
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startTime = startOfWeek.atStartOfDay();
        LocalDateTime endTime = endOfWeek.atTime(23, 59, 59);

        return generateReport(projectId, "WEEKLY", startTime, endTime);
    }

    /**
     * 生成项目月报
     *
     * @param projectId 项目ID
     * @return 月报数据
     */
    public ProjectReportDTO generateMonthlyReport(Long projectId) {
        log.info("生成项目月报: projectId={}", projectId);

        // 计算本月的起止日期
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());

        LocalDateTime startTime = startOfMonth.atStartOfDay();
        LocalDateTime endTime = endOfMonth.atTime(23, 59, 59);

        return generateReport(projectId, "MONTHLY", startTime, endTime);
    }

    /**
     * 生成报告（通用方法）
     *
     * @param projectId 项目ID
     * @param reportType 报告类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报告数据
     */
    private ProjectReportDTO generateReport(Long projectId, String reportType,
                                            LocalDateTime startTime, LocalDateTime endTime) {
        ProjectReportDTO report = new ProjectReportDTO();
        report.setProjectId(projectId);
        report.setReportType(reportType);
        report.setStartDate(startTime.toLocalDate());
        report.setEndDate(endTime.toLocalDate());
        report.setGenerateTime(LocalDateTime.now());

        // 1. 获取周期内的需求列表
        List<Map<String, Object>> demands = projectMapper.getDemandsInPeriod(projectId, startTime, endTime);
        report.setDemands(convertToReportDemands(demands));

        // 2. 获取周期内完成的需求
        List<Map<String, Object>> completedDemands = projectMapper.getCompletedDemandsInPeriod(
                projectId, startTime, endTime);
        report.setCompletedDemands(convertToReportDemands(completedDemands));

        // 3. 获取成员工作量
        List<Map<String, Object>> memberWorkloads = projectMapper.getMemberWorkloadInPeriod(
                projectId, startTime, endTime);
        report.setMemberWorkloads(convertToMemberWorkloads(memberWorkloads));

        // 4. 计算总体统计
        ProjectReportDTO.ReportSummary summary = calculateSummary(demands, completedDemands, memberWorkloads);
        report.setSummary(summary);

        // 5. 计算状态分布
        Map<String, Integer> statusDist = demands.stream()
                .collect(Collectors.groupingBy(
                        d -> (String) d.get("status"),
                        Collectors.summingInt(d -> 1)
                ));
        report.setStatusDistribution(statusDist);

        // 6. 计算类型分布
        Map<String, Integer> typeDist = demands.stream()
                .collect(Collectors.groupingBy(
                        d -> (String) d.get("type"),
                        Collectors.summingInt(d -> 1)
                ));
        report.setTypeDistribution(typeDist);

        // 7. 筛选进行中的需求和新增需求
        report.setInProgressDemands(report.getDemands().stream()
                .filter(d -> "IN_DEVELOPMENT".equals(d.getStatus()) || "IN_TEST".equals(d.getStatus()))
                .collect(Collectors.toList()));

        report.setNewDemands(report.getDemands().stream()
                .filter(d -> d.getCreateTime() != null &&
                        !d.getCreateTime().isBefore(startTime.toLocalDate()) &&
                        !d.getCreateTime().isAfter(endTime.toLocalDate()))
                .collect(Collectors.toList()));

        log.info("报告生成完成: projectId={}, type={}, demandCount={}",
                projectId, reportType, demands.size());

        return report;
    }

    /**
     * 计算总体统计
     */
    private ProjectReportDTO.ReportSummary calculateSummary(List<Map<String, Object>> demands,
                                                            List<Map<String, Object>> completedDemands,
                                                            List<Map<String, Object>> memberWorkloads) {
        ProjectReportDTO.ReportSummary summary = new ProjectReportDTO.ReportSummary();

        summary.setTotalDemands(demands.size());
        summary.setCompletedCount(completedDemands.size());
        summary.setInProgressCount((int) demands.stream()
                .filter(d -> "IN_DEVELOPMENT".equals(d.get("status")) ||
                        "IN_TEST".equals(d.get("status")))
                .count());
        summary.setNewCount((int) demands.stream()
                .filter(d -> d.get("create_time") != null)
                .count());

        double completionRate = demands.isEmpty() ? 0 :
                completedDemands.size() * 100.0 / demands.size();
        summary.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);

        double totalHours = memberWorkloads.stream()
                .mapToDouble(m -> ((Number) m.get("actualHours")).doubleValue())
                .sum();
        summary.setTotalHours(Math.round(totalHours * 100.0) / 100.0);

        double avgHours = memberWorkloads.isEmpty() ? 0 :
                totalHours / memberWorkloads.size();
        summary.setAvgHoursPerPerson(Math.round(avgHours * 100.0) / 100.0);

        return summary;
    }

    /**
     * 转换为报告需求列表
     */
    private List<ProjectReportDTO.ReportDemand> convertToReportDemands(List<Map<String, Object>> demands) {
        return demands.stream().map(d -> {
            ProjectReportDTO.ReportDemand demand = new ProjectReportDTO.ReportDemand();
            demand.setId(((Number) d.get("id")).longValue());
            demand.setTitle((String) d.get("title"));
            demand.setType((String) d.get("type"));
            demand.setPriority((String) d.get("priority"));
            demand.setStatus((String) d.get("status"));
            demand.setAssigneeName((String) d.get("assigneeName"));

            if (d.get("create_time") != null) {
                demand.setCreateTime(((java.sql.Timestamp) d.get("create_time")).toLocalDateTime().toLocalDate());
            }
            if (d.get("complete_time") != null) {
                demand.setCompleteTime(((java.sql.Timestamp) d.get("complete_time")).toLocalDateTime().toLocalDate());
            }

            demand.setEstimatedHours(d.get("estimated_hours") != null ?
                    ((Number) d.get("estimated_hours")).doubleValue() : 0);
            demand.setActualHours(d.get("actual_hours") != null ?
                    ((Number) d.get("actual_hours")).doubleValue() : 0);

            return demand;
        }).collect(Collectors.toList());
    }

    /**
     * 转换为成员工作量列表
     */
    private List<ProjectReportDTO.MemberWorkload> convertToMemberWorkloads(List<Map<String, Object>> workloads) {
        return workloads.stream().map(w -> {
            ProjectReportDTO.MemberWorkload workload = new ProjectReportDTO.MemberWorkload();
            workload.setUserId(w.get("userId") != null ? ((Number) w.get("userId")).longValue() : null);
            workload.setUserName((String) w.get("userName"));
            workload.setDemandCount(((Number) w.get("demandCount")).intValue());
            workload.setCompletedCount(((Number) w.get("completedCount")).intValue());
            workload.setEstimatedHours(((Number) w.get("estimatedHours")).doubleValue());
            workload.setActualHours(((Number) w.get("actualHours")).doubleValue());

            double rate = workload.getEstimatedHours() > 0 ?
                    workload.getActualHours() * 100.0 / workload.getEstimatedHours() : 0;
            workload.setCompletionRate(Math.round(rate * 100.0) / 100.0);

            return workload;
        }).collect(Collectors.toList());
    }
}
