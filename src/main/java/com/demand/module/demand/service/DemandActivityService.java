package com.demand.module.demand.service;

import com.demand.module.demand.entity.DemandActivity;
import com.demand.module.demand.mapper.DemandActivityMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 需求动态时间轴业务服务
 * <p>
 * 负责记录需求的所有关键操作，形成完整的审计时间轴。
 * 每个操作都会记录操作人、动作类型、内容描述和时间戳。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>保存动态</b>：记录需求的创建、审批、分配、状态变更等操作</li>
 *   <li><b>查询动态</b>：返回需求的所有操作记录，按时间倒序排列</li>
 *   <li><b>填充头像</b>：自动查询操作人的头像 URL，方便前端展示</li>
 * </ul>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>需求详情页展示操作历史时间轴</li>
 *   <li>审计追踪：查看谁在什么时间执行了什么操作</li>
 *   <li>故障排查：定位需求状态变更的原因</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DemandActivityService {

    /**
     * 需求动态数据访问层
     */
    private final DemandActivityMapper activityMapper;

    /**
     * 用户数据访问层，用于查询操作人头像
     */
    private final UserMapper userMapper;

    /**
     * 保存需求动态记录
     * <p>
     * 在需求的关键操作后调用此方法，记录操作信息。
     * </p>
     * 
     * <p>
     * <b>调用时机</b>：
     * <ul>
     *   <li>创建需求后</li>
     *   <li>提交审核后</li>
     *   <li>审批通过后</li>
     *   <li>分配负责人后</li>
     *   <li>状态变更后</li>
     *   <li>上传/删除附件后</li>
     * </ul>
     * </p>
     *
     * @param demandId     需求 ID
     * @param operatorId   操作人 ID
     * @param operatorName 操作人姓名
     * @param actionType   动作类型（如 CREATE、APPROVE、ASSIGN）
     * @param content      动态内容描述
     * @param extraData    扩展数据（JSON 格式，可选）
     */
    public void saveActivity(Long demandId, Long operatorId, String operatorName,
                             String actionType, String content, String extraData) {
        DemandActivity activity = new DemandActivity();
        activity.setDemandId(demandId);
        activity.setOperatorId(operatorId);
        activity.setOperatorName(operatorName);
        activity.setActionType(actionType);
        activity.setContent(content);
        activity.setExtraData(extraData);
        activityMapper.insert(activity);
    }

    /**
     * 查询需求的所有动态记录
     * <p>
     * 返回指定需求的所有操作记录，按创建时间倒序排列（最新的在前）。
     * 同时填充每个操作人的头像 URL。
     * </p>
     * 
     * <p>
     * <b>数据处理</b>：
     * <ol>
     *   <li>从数据库查询动态列表</li>
     *   <li>遍历每条动态，查询操作人的头像</li>
     *   <li>返回完整的动态列表</li>
     * </ol>
     * </p>
     *
     * @param demandId 需求 ID
     * @return 动态列表（包含 operatorAvatar 字段）
     */
    public List<DemandActivity> getActivitiesByDemandId(Long demandId) {
        List<DemandActivity> activities = activityMapper.selectByDemandId(demandId);
        
        // 填充操作人头像
        for (DemandActivity activity : activities) {
            if (activity.getOperatorId() != null) {
                User user = userMapper.findById(activity.getOperatorId());
                if (user != null && user.getAvatar() != null) {
                    activity.setOperatorAvatar(user.getAvatar());
                }
            }
        }
        return activities;
    }
}
