package com.demand.module.comment.service;

import com.demand.exception.BusinessException;
import com.demand.module.comment.entity.Comment;
import com.demand.module.comment.entity.CommentLike;
import com.demand.module.comment.mapper.CommentLikeMapper;
import com.demand.module.comment.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论点赞业务服务
 * <p>
 * 负责管理评论的点赞和取消点赞操作。
 * 支持批量查询点赞数和点赞状态，提升性能。
 * </p>
 *
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>点赞</b>：用户对评论进行点赞，防止重复点赞</li>
 *   <li><b>取消点赞</b>：用户取消对评论的点赞</li>
 *   <li><b>批量填充点赞信息</b>：一次性查询多条评论的点赞数和点赞状态</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeMapper commentLikeMapper;
    private final CommentMapper commentMapper;

    /**
     * 点赞评论
     * <p>
     * 用户对指定评论进行点赞。如果已经点过赞，则抛出异常。
     * </p>
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @throws BusinessException 当评论不存在或已点赞时抛出
     */
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        log.info("点赞评论: commentId={}, userId={}", commentId, userId);

        // 1. 检查评论是否存在
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 2. 检查是否已经点过赞
        CommentLike existingLike = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
        if (existingLike != null) {
            throw new BusinessException("您已经点过赞了");
        }

        // 3. 插入点赞记录
        CommentLike commentLike = new CommentLike();
        commentLike.setCommentId(commentId);
        commentLike.setUserId(userId);
        commentLike.setCreateTime(LocalDateTime.now());
        commentLikeMapper.insert(commentLike);

        log.info("点赞成功: commentId={}, userId={}", commentId, userId);
    }

    /**
     * 取消点赞
     * <p>
     * 用户取消对指定评论的点赞。如果未点过赞，则抛出异常。
     * </p>
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @throws BusinessException 当评论不存在或未点赞时抛出
     */
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        log.info("取消点赞: commentId={}, userId={}", commentId, userId);

        // 1. 检查评论是否存在
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 2. 检查是否点过赞
        CommentLike existingLike = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
        if (existingLike == null) {
            throw new BusinessException("您还未点赞");
        }

        // 3. 删除点赞记录
        commentLikeMapper.delete(commentId, userId);

        log.info("取消点赞成功: commentId={}, userId={}", commentId, userId);
    }

    /**
     * 批量填充评论的点赞信息
     * <p>
     * 为评论列表填充点赞数（likeCount）和当前用户的点赞状态（liked）。
     * 使用批量查询优化性能，避免 N+1 查询问题。
     * </p>
     *
     * @param comments 评论列表
     * @param currentUserId 当前用户 ID（可为 null，表示未登录用户）
     */
    public void fillLikeInfo(List<Comment> comments, Long currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        // 提取所有评论 ID
        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        // 1. 批量查询点赞数
        List<Map<String, Object>> likeCounts = commentLikeMapper.batchCountByCommentIds(commentIds);
        Map<Long, Integer> countMap = likeCounts.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("comment_id")).longValue(),
                        m -> ((Number) m.get("count")).intValue()
                ));

        // 2. 批量查询当前用户的点赞状态
        Set<Long> likedCommentIds = Set.of();
        if (currentUserId != null) {
            likedCommentIds = commentLikeMapper.findLikedCommentIds(commentIds, currentUserId);
        }

        final Set<Long> finalLikedCommentIds = likedCommentIds;

        // 3. 填充到评论对象
        comments.forEach(comment -> {
            // 设置点赞数
            comment.setLikeCount(countMap.getOrDefault(comment.getId(), 0));

            // 设置点赞状态
            comment.setLiked(finalLikedCommentIds.contains(comment.getId()));
        });

        log.debug("批量填充点赞信息完成: commentCount={}", comments.size());
    }
}
