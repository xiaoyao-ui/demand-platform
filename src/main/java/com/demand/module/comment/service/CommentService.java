package com.demand.module.comment.service;

import com.demand.module.comment.entity.Comment;
import com.demand.module.comment.mapper.CommentMapper;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.notification.service.NotificationService;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论业务逻辑服务
 * <p>
 * 负责评论的添加、查询和删除。
 * 集成通知系统，当用户发表评论时自动通知需求提出人和负责人。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>添加评论</b>：保存评论并发送通知给相关人员</li>
 *   <li><b>查询评论</b>：返回需求的评论列表，并填充评论人头像</li>
 *   <li><b>删除评论</b>：物理删除评论记录</li>
 * </ul>
 * 
 * <h3>通知机制：</h3>
 * <p>
 * 当用户发表评论时，系统会自动发送通知给：
 * <ul>
 *   <li><b>需求提出人</b>：收到"新评论"通知，内容为"XXX 在您的需求【YYY】下发表了评论"</li>
 *   <li><b>需求负责人</b>：收到"新评论"通知，内容为"XXX 在您负责的需求【YYY】下发表了评论"</li>
 * </ul>
 * 通知通过 WebSocket 实时推送，确保相关人员及时获知讨论动态。
 * </p>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>如果评论人就是提出人或负责人，不会给自己发送通知</li>
 *   <li>通知类型为 2（评论通知），前端可根据类型显示不同图标</li>
 *   <li>relatedId 字段存储需求 ID，点击通知可直接跳转到需求详情页</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    /**
     * 评论数据访问层
     */
    private final CommentMapper commentMapper;

    /**
     * 通知服务，用于发送评论通知
     */
    private final NotificationService notificationService;

    /**
     * 用户数据访问层，用于查询评论人信息和头像
     */
    private final UserMapper userMapper;

    /**
     * 需求数据访问层，用于查询需求提出人和负责人
     */
    private final DemandMapper demandMapper;

    /**
     * 评论点赞服务
     */
    private final CommentLikeService commentLikeService;

    /**
     * 权限服务，用于获取当前用户 ID
     */
    private final com.demand.module.user.service.PermissionService permissionService;

    /**
     * 添加评论
     * <p>
     * 保存评论并发送通知给需求提出人和负责人。
     * </p>
     * 
     * <p>
     * <b>业务流程</b>：
     * <ol>
     *   <li>设置创建时间和更新时间</li>
     *   <li>插入数据库</li>
     *   <li>查询需求信息，获取提出人和负责人 ID</li>
     *   <li>查询评论人信息，获取姓名</li>
     *   <li>发送通知给提出人（如果评论人不是提出人）</li>
     *   <li>发送通知给负责人（如果评论人不是负责人）</li>
     * </ol>
     * </p>
     * 
     * <p>
     * <b>通知示例</b>：
     * <ul>
     *   <li>标题："新评论"</li>
     *   <li>内容："张三 在您的需求【用户登录功能】下发表了评论"</li>
     *   <li>类型：2（评论通知）</li>
     *   <li>关联 ID：需求 ID</li>
     * </ul>
     * </p>
     *
     * @param comment 评论对象
     */
    @Transactional
    public void addComment(Comment comment) {
        log.info("添加评论: demandId={}, userId={}", comment.getDemandId(), comment.getUserId());
        
        // 1. 初始化评论信息
        initComment(comment);
        
        // 2. 保存评论到数据库
        commentMapper.insert(comment);
        log.info("评论保存成功: commentId={}", comment.getId());

        // 3. 异步发送通知（不阻塞主流程）
        sendCommentNotifications(comment);
    }

    /**
     * 初始化评论信息
     *
     * @param comment 评论对象
     */
    private void initComment(Comment comment) {
        LocalDateTime now = LocalDateTime.now();
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
    }

    /**
     * 发送评论通知
     * <p>
     * 通知需求提出人和负责人有新评论。
     * 如果评论人就是提出人或负责人，则不发送通知给自己。
     * </p>
     *
     * @param comment 评论对象
     */
    private void sendCommentNotifications(Comment comment) {
        try {
            // 1. 查询需求信息
            Demand demand = demandMapper.findById(comment.getDemandId());
            if (demand == null) {
                log.warn("需求不存在，无法发送通知: demandId={}", comment.getDemandId());
                return;
            }

            // 2. 查询评论人信息
            User commenter = userMapper.findById(comment.getUserId());
            String commenterName = commenter != null ? commenter.getRealName() : "用户";

            // 3. 通知提出人
            notifyProposer(demand, commenterName, comment.getUserId());

            // 4. 通知负责人
            notifyAssignee(demand, commenterName, comment.getUserId());
            
        } catch (Exception e) {
            // 通知失败不影响评论保存，仅记录日志
            log.error("发送评论通知失败: commentId={}", comment.getId(), e);
        }
    }

    /**
     * 通知需求提出人
     *
     * @param demand 需求对象
     * @param commenterName 评论人姓名
     * @param commenterId 评论人 ID
     */
    private void notifyProposer(Demand demand, String commenterName, Long commenterId) {
        Long proposerId = demand.getCreatorId();
        
        // 如果提出人存在且不是评论人本人，则发送通知
        if (proposerId != null && !proposerId.equals(commenterId)) {
            log.debug("发送新评论通知给提出人: demandId={}, proposerId={}", demand.getId(), proposerId);
            
            String content = String.format("%s 在您的需求【%s】下发表了评论", commenterName, demand.getTitle());
            notificationService.sendNotification(
                    commenterId,      // 发送人：评论人
                    proposerId,       // 接收人：提出人
                    "新评论",
                    content,
                    2,                // 类型：评论通知
                    demand.getId()    // 关联 ID：需求 ID
            );
        }
    }

    /**
     * 通知需求负责人
     *
     * @param demand 需求对象
     * @param commenterName 评论人姓名
     * @param commenterId 评论人 ID
     */
    private void notifyAssignee(Demand demand, String commenterName, Long commenterId) {
        Long assigneeId = demand.getAssigneeId();
        
        // 如果负责人存在且不是评论人本人，则发送通知
        if (assigneeId != null && !assigneeId.equals(commenterId)) {
            log.debug("发送新评论通知给负责人: demandId={}, assigneeId={}", demand.getId(), assigneeId);
            
            String content = String.format("%s 在您负责的需求【%s】下发表了评论", commenterName, demand.getTitle());
            notificationService.sendNotification(
                    commenterId,      // 发送人：评论人
                    assigneeId,       // 接收人：负责人
                    "新评论",
                    content,
                    2,                // 类型：评论通知
                    demand.getId()    // 关联 ID：需求 ID
            );
        }
    }

    /**
     * 查询需求的评论列表
     * <p>
     * 返回指定需求的所有评论，并填充评论人头像和点赞信息。
     * </p>
     * 
     * <p>
     * <b>数据处理</b>：
     * <ul>
     *   <li>从数据库查询评论列表（已包含 userName 字段）</li>
     *   <li>遍历评论，查询每个评论人的头像 URL</li>
     *   <li>批量查询点赞数和当前用户的点赞状态</li>
     *   <li>返回完整的评论列表</li>
     * </ul>
     * </p>
     *
     * @param demandId 需求 ID
     * @return 评论列表（包含 userName、userAvatar、likeCount、liked 字段）
     */
    public List<Comment> getCommentsByDemandId(Long demandId) {
        log.debug("查询需求评论: demandId={}", demandId);
        List<Comment> comments = commentMapper.findByDemandId(demandId);
        
        if (comments.isEmpty()) {
            return comments;
        }
        
        // 填充评论人头像
        comments.forEach(comment -> {
            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                comment.setUserAvatar(user.getAvatar());
            }
        });
        
        // 获取当前用户 ID
        Long currentUserId = null;
        try {
            currentUserId = permissionService.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户，currentUserId 为 null
            log.debug("用户未登录，不填充点赞状态");
        }
        
        // 批量填充点赞信息
        commentLikeService.fillLikeInfo(comments, currentUserId);
        
        return comments;
    }

    /**
     * 删除评论
     * <p>
     * 物理删除评论记录。
     * 注意：此方法不会级联删除子回复。
     * </p>
     *
     * @param id 评论 ID
     */
    public void deleteComment(Long id) {
        log.info("删除评论: commentId={}", id);
        commentMapper.deleteById(id);
        log.info("评论删除成功: commentId={}", id);
    }
}
