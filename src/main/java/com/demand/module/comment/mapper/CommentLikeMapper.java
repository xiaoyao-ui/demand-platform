package com.demand.module.comment.mapper;

import com.demand.module.comment.entity.CommentLike;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

/**
 * 评论点赞数据访问层
 * <p>
 * 提供点赞记录的增删查功能。
 * </p>
 */
@Mapper
public interface CommentLikeMapper {

    /**
     * 添加点赞
     *
     * @param commentLike 点赞对象
     * @return 影响的行数
     */
    @Insert("INSERT INTO comment_like(comment_id, user_id, create_time) " +
            "VALUES(#{commentId}, #{userId}, #{createTime})")
    int insert(CommentLike commentLike);

    /**
     * 取消点赞
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int delete(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 检查用户是否已点赞
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @return 点赞记录，不存在则返回 null
     */
    @Select("SELECT * FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    CommentLike findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 统计评论的点赞数
     *
     * @param commentId 评论 ID
     * @return 点赞数量
     */
    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId}")
    int countByCommentId(Long commentId);

    /**
     * 批量统计多个评论的点赞数
     *
     * @param commentIds 评论 ID 列表
     * @return 点赞数统计结果（comment_id -> count）
     */
    @Select("<script>" +
            "SELECT comment_id, COUNT(*) as count " +
            "FROM comment_like " +
            "WHERE comment_id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "GROUP BY comment_id" +
            "</script>")
    List<java.util.Map<String, Object>> batchCountByCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 批量查询用户的点赞状态
     *
     * @param commentIds 评论 ID 列表
     * @param userId 用户 ID
     * @return 用户已点赞的评论 ID 集合
     */
    @Select("<script>" +
            "SELECT comment_id FROM comment_like " +
            "WHERE comment_id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "AND user_id = #{userId}" +
            "</script>")
    Set<Long> findLikedCommentIds(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);
}
