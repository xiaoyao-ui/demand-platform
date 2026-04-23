package com.demand.module.user.service;

import com.demand.common.PageResult;
import com.demand.module.user.entity.OperationLog;
import com.demand.module.user.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志业务逻辑服务
 * <p>
 * 负责操作日志的分页查询和全量导出功能。
 * 支持按用户名、请求路径、操作描述、状态码、时间范围等多条件组合筛选。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    /**
     * 操作日志数据访问层
     */
    private final OperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志
     * <p>
     * 根据筛选条件从数据库中提取日志记录，并计算总记录数用于前端分页展示。
     * </p>
     *
     * @param username  用户名（模糊匹配）
     * @param uri       请求路径（模糊匹配）
     * @param operation 操作描述（模糊匹配）
     * @param status    响应状态码
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @return 分页结果对象
     */
    public PageResult<OperationLog> queryLogs(String username, String uri, String operation, 
                                              Integer status, LocalDateTime startTime, LocalDateTime endTime,
                                              int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<OperationLog> list = operationLogMapper.selectPage(username, uri, operation, status, startTime, endTime, offset, pageSize);
        long total = operationLogMapper.countTotal(username, uri, operation, status, startTime, endTime);
        return new PageResult<>(list, total, pageNum, pageSize);
    }

    /**
     * 查询所有符合条件的日志（用于 Excel 导出）
     * <p>
     * 不分页返回所有匹配的日志记录，适用于数据量较小的导出场景。
     * </p>
     *
     * @param username  用户名
     * @param uri       请求路径
     * @param operation 操作描述
     * @param status    响应状态码
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    public List<OperationLog> queryAllLogs(String username, String uri, String operation,
                                           Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        return operationLogMapper.selectAll(username, uri, operation, status, startTime, endTime);
    }
}
