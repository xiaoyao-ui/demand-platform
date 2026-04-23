package com.demand.module.comment.mapper;

import com.demand.module.comment.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 评论数据访问层（Mapper）
 * <p>
 * 提供评论数据的增删查功能。
 * 所有 SQL 语句使用注解方式定义，无需 XML 配置文件。
 * </p>
 * 
 * <h3>核心方法：</h3>
 * <ul>
 *   <li>{@link #insert} - 插入新评论</li>
 *   <li>{@link #findByDemandId} - 查询需求的所有评论（含评论人姓名）</li>
 *   <li>{@link #deleteById} - 删除评论</li>
 * </ul>
 */
@Mapper
public interface CommentMapper {

    /**
     * 插入评论
     * <p>
     * 新增评论记录，自动生成主键 ID。
     * </p>
     *
     * @param comment 评论对象（包含 demandId、userId、content、parentId 等字段）
     * @return 影响的行数（始终为 1）
     */
    @Insert("INSERT INTO comment(demand_id, user_id, content, parent_id, create_time, update_time) " +
            "VALUES(#{demandId}, #{userId}, #{content}, #{parentId}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    /**
     * 查询需求的所有评论
     * <p>
     * 通过 LEFT JOIN 关联 {@code user} 表，获取评论人的真实姓名。
     * 评论按创建时间升序排列（最早的在前），符合常规聊天界面的展示逻辑。
     * </p>
     * 
     * <p>
     * <b>SQL 示例</b>：
     * <pre>{@code
     * SELECT c.*, u.real_name AS userName
     * FROM comment c
     * LEFT JOIN user u ON c.user_id = u.id
     * WHERE c.demand_id = 100
     * ORDER BY c.create_time ASC
     * }</pre>
     * </p>
     *
     * @param demandId 需求 ID
     * @return 评论列表（包含 userName 字段）
     */
    @Select("SELECT c.*, u.real_name AS userName " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.demand_id = #{demandId} " +
            "ORDER BY c.create_time ASC")
    List<Comment> findByDemandId(Long demandId);

    /**
     * 删除评论
     * <p>
     * 物理删除评论记录。
     * 注意：此方法不会级联删除子回复，子回复的 parentId 将指向不存在的记录。
     * </p>
     *
     * @param id 评论 ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM comment WHERE id = #{id}")
    int deleteById(Long id);
}
