package com.demand.module.user.mapper;

import com.demand.module.user.entity.OperationLog;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层（Mapper）
 * <p>
 * 提供操作日志的增删查改功能，支持多条件组合筛选和分页查询。
 * SQL 语句定义在 {@code src/main/resources/mapper/OperationLogMapper.xml} 中。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>日志记录</b>：插入新的操作日志（由 AOP 切面自动调用）</li>
 *   <li><b>分页查询</b>：支持按用户名、路径、操作描述、状态、时间范围筛选</li>
 *   <li><b>全量导出</b>：查询所有符合条件的日志（用于 Excel 导出）</li>
 *   <li><b>定期清理</b>：删除 90 天前的历史日志，释放数据库空间</li>
 * </ul>
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 插入操作日志
     * <p>
     * 由 {@link com.demand.config.AuditLogAspect} 在 Controller 方法执行后自动调用。
     * 记录用户操作的关键信息，包括请求参数、响应状态、IP 地址等。
     * </p>
     *
     * @param log 操作日志实体对象
     * @return 影响的行数（始终为 1）
     */
    @Insert("INSERT INTO operation_log(user_id, username, operation, method, uri, params, ip, status, error_msg, create_time) " +
            "VALUES(#{userId}, #{username}, #{operation}, #{method}, #{uri}, #{params}, #{ip}, #{status}, #{errorMsg}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    /**
     * 分页查询操作日志
     * <p>
     * 根据筛选条件从数据库中提取日志记录，支持模糊匹配和时间范围查询。
     * SQL 中使用 {@code LIMIT #{offset}, #{limit}} 实现物理分页。
     * </p>
     * 
     * <p>
     * <b>筛选规则</b>：
     * <ul>
     *   <li>{@code username}：模糊匹配操作用户名</li>
     *   <li>{@code uri}：模糊匹配请求路径</li>
     *   <li>{@code operation}：模糊匹配操作描述</li>
     *   <li>{@code status}：精确匹配响应状态码</li>
     *   <li>{@code startTime/endTime}：时间范围筛选（create_time >= startTime AND create_time <= endTime）</li>
     * </ul>
     * </p>
     *
     * @param username  用户名（可选）
     * @param uri       请求路径（可选）
     * @param operation 操作描述（可选）
     * @param status    响应状态码（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param offset    偏移量（pageNum - 1）* pageSize
     * @param limit     每页条数
     * @return 当前页的日志列表
     */
    List<OperationLog> selectPage(@Param("username") String username,
                                   @Param("uri") String uri,
                                   @Param("operation") String operation,
                                   @Param("status") Integer status,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 查询符合条件的日志总数
     * <p>
     * 用于前端分页组件计算总页数。
     * 筛选条件与 {@link #selectPage} 保持一致。
     * </p>
     *
     * @param username  用户名
     * @param uri       请求路径
     * @param operation 操作描述
     * @param status    响应状态码
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 符合条件的总记录数
     */
    long countTotal(@Param("username") String username,
                    @Param("uri") String uri,
                    @Param("operation") String operation,
                    @Param("status") Integer status,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询所有符合条件的日志（用于 Excel 导出）
     * <p>
     * 不分页返回所有匹配的日志记录，适用于数据量较小的导出场景。
     * 如果数据量超过 10000 条，建议增加时间范围限制或分批导出。
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
    List<OperationLog> selectAll(@Param("username") String username,
                                  @Param("uri") String uri,
                                  @Param("operation") String operation,
                                  @Param("status") Integer status,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的操作日志
     * <p>
     * 用于定时任务清理历史数据，保留最近 90 天的日志。
     * 由 {@link com.demand.config.ScheduledTasks#cleanOldOperationLogs()} 每天凌晨 3 点自动执行。
     * </p>
     * 
     * <p>
     * <b>使用示例</b>：
     * <pre>{@code
     * // 删除 90 天前的日志
     * LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90);
     * int deletedCount = mapper.deleteBeforeTime(cutoffTime);
     * }</pre>
     * </p>
     *
     * @param beforeTime 截止时间（删除此时间之前的所有日志）
     * @return 删除的记录数
     */
    @Delete("DELETE FROM operation_log WHERE create_time < #{beforeTime}")
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
}