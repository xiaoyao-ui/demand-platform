package com.demand.module.notification.mapper;

import com.demand.module.notification.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 通知消息数据访问层（Mapper）
 * <p>
 * 提供通知数据的增删查改功能。
 * 所有 SQL 语句使用注解方式定义，无需 XML 配置文件。
 * </p>
 * 
 * <h3>核心方法：</h3>
 * <ul>
 *   <li>{@link #insert} - 创建新通知</li>
 *   <li>{@link #findByReceiverId} - 分页查询用户的通知列表</li>
 *   <li>{@link #countUnreadByReceiverId} - 统计未读数量</li>
 *   <li>{@link #markAsRead/markAllAsRead/batchMarkAsRead} - 标记已读</li>
 * </ul>
 */
@Mapper
public interface NotificationMapper {

    /**
     * 插入通知
     * <p>
     * 创建一条新的通知记录，自动生成主键 ID。
     * 由 {@link com.demand.module.notification.service.NotificationService#sendNotification} 调用。
     * </p>
     *
     * @param notification 通知对象
     * @return 影响的行数（始终为 1）
     */
    @Insert("INSERT INTO notification(user_id, receiver_id, title, content, type, related_id, is_read, create_time) " +
            "VALUES(#{userId}, #{receiverId}, #{title}, #{content}, #{type}, #{relatedId}, #{isRead}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    /**
     * 分页查询用户的通知列表
     * <p>
     * 按创建时间倒序排列，最新的通知在前。
     * 使用 {@code LIMIT #{offset}, #{pageSize}} 实现物理分页。
     * </p>
     *
     * @param receiverId 接收人 ID
     * @param offset     偏移量（pageNum - 1）* pageSize
     * @param pageSize   每页条数
     * @return 通知列表
     */
    @Select("SELECT * FROM notification WHERE receiver_id = #{receiverId} ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<Notification> findByReceiverId(@Param("receiverId") Long receiverId,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    /**
     * 查询用户的通知总数
     * <p>
     * 用于前端分页组件计算总页数。
     * </p>
     *
     * @param receiverId 接收人 ID
     * @return 通知总数
     */
    @Select("SELECT COUNT(*) FROM notification WHERE receiver_id = #{receiverId}")
    Long countByReceiverId(Long receiverId);

    /**
     * 查询用户的未读通知数量
     * <p>
     * 用于前端在通知图标上显示红色角标。
     * </p>
     *
     * @param receiverId 接收人 ID
     * @return 未读通知数量
     */
    @Select("SELECT COUNT(*) FROM notification WHERE receiver_id = #{receiverId} AND is_read = 0")
    Long countUnreadByReceiverId(Long receiverId);

    /**
     * 标记单条通知为已读
     * <p>
     * 将指定通知的 {@code is_read} 字段设置为 1。
     * </p>
     *
     * @param id 通知 ID
     * @return 影响的行数
     */
    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id}")
    int markAsRead(Long id);

    /**
     * 标记用户的所有通知为已读
     * <p>
     * 一键将所有未读通知标记为已读，常用于"全部已读"按钮。
     * </p>
     *
     * @param receiverId 接收人 ID
     * @return 影响的行数
     */
    @Update("UPDATE notification SET is_read = 1 WHERE receiver_id = #{receiverId}")
    int markAllAsRead(Long receiverId);

    /**
     * 批量标记通知为已读
     * <p>
     * 使用 MyBatis 动态 SQL 的 {@code <foreach>} 标签生成 IN 子句。
     * </p>
     * 
     * <p>
     * <b>SQL 示例</b>：
     * <pre>{@code
     * UPDATE notification SET is_read = 1 
     * WHERE id IN (1, 2, 3, 4, 5)
     * }</pre>
     * </p>
     *
     * @param ids 通知 ID 列表
     */
    @Update("<script>UPDATE notification SET is_read = 1 WHERE id IN " +
            "<foreach item='id' index='index' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach></script>")
    void batchMarkAsRead(@Param("ids") List<Long> ids);
}
