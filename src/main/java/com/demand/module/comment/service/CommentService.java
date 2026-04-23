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
    public void addComment(Comment comment) {
        log.info("添加评论: demandId={}, userId={}, content={}", 
                comment.getDemandId(), comment.getUserId(), comment.getContent());
        
        // 1. 设置时间戳
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        
        // 2. 插入数据库
        commentMapper.insert(comment);

        // 3. 查询需求信息
        Demand demand = demandMapper.findById(comment.getDemandId());
        if (demand != null) {
            // 4. 查询评论人信息
            User commenter = userMapper.findById(comment.getUserId());
            String commenterName = commenter != null ? commenter.getRealName() : "用户";

            // 5. 通知提出人（如果评论人不是提出人）
            if (demand.getProposerId() != null && !demand.getProposerId().equals(comment.getUserId())) {
                log.info("发送新评论通知给提出人: demandId={}, proposerId={}", 
                        demand.getId(), demand.getProposerId());
                notificationService.sendNotification(
                        comment.getUserId(),        // 发送人：评论人
                        demand.getProposerId(),     // 接收人：提出人
                        "新评论",
                        commenterName + " 在您的需求【" + demand.getTitle() + "】下发表了评论",
                        2,                          // 类型：评论通知
                        comment.getDemandId()       // 关联 ID：需求 ID
                );
            }

            // 6. 通知负责人（如果评论人不是负责人）
            if (demand.getAssigneeId() != null && !demand.getAssigneeId().equals(comment.getUserId())) {
                log.info("发送新评论通知给负责人: demandId={}, assigneeId={}", 
                        demand.getId(), demand.getAssigneeId());
                notificationService.sendNotification(
                        comment.getUserId(),        // 发送人：评论人
                        demand.getAssigneeId(),     // 接收人：负责人
                        "新评论",
                        commenterName + " 在您负责的需求【" + demand.getTitle() + "】下发表了评论",
                        2,                          // 类型：评论通知
                        comment.getDemandId()       // 关联 ID：需求 ID
                );
            }
        }
        log.info("评论添加成功: commentId={}", comment.getId());
    }

    /**
     * 查询需求的评论列表
     * <p>
     * 返回指定需求的所有评论，并填充评论人头像。
     * </p>
     * 
     * <p>
     * <b>数据处理</b>：
     * <ul>
     *   <li>从数据库查询评论列表（已包含 userName 字段）</li>
     *   <li>遍历评论，查询每个评论人的头像 URL</li>
     *   <li>返回完整的评论列表</li>
     * </ul>
     * </p>
     *
     * @param demandId 需求 ID
     * @return 评论列表（包含 userName 和 userAvatar 字段）
     */
    public List<Comment> getCommentsByDemandId(Long demandId) {
        log.debug("查询需求评论: demandId={}", demandId);
        List<Comment> comments = commentMapper.findByDemandId(demandId);
        
        // 填充评论人头像
        comments.forEach(comment -> {
            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                comment.setUserAvatar(user.getAvatar());
            }
        });
        
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
