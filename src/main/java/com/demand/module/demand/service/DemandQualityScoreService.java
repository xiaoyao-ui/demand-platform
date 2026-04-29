package com.demand.module.demand.service;

import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandQualityScore;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.demand.mapper.DemandQualityScoreMapper;
import com.demand.module.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 需求质量评分服务
 * <p>
 * 对需求的完整性和清晰度进行自动评分，识别低质量需求并提醒改进。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandQualityScoreService {

    private final DemandQualityScoreMapper scoreMapper;
    private final DemandMapper demandMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * 评分权重配置
     */
    private static final int REQUIRED_FIELDS_MAX = 30;
    private static final int DESCRIPTION_MAX = 25;
    private static final int ACCEPTANCE_CRITERIA_MAX = 20;
    private static final int TECHNICAL_DETAILS_MAX = 15;
    private static final int BUSINESS_VALUE_MAX = 10;
    private static final int LOW_QUALITY_THRESHOLD = 60;

    /**
     * 计算需求质量评分
     *
     * @param demandId 需求 ID
     * @return 评分对象
     */
    @Transactional
    public DemandQualityScore calculateScore(Long demandId) {
        log.info("计算需求质量评分: demandId={}", demandId);

        Demand demand = demandMapper.findById(demandId);
        if (demand == null) {
            throw new RuntimeException("需求不存在");
        }

        // 1. 计算各维度得分
        int requiredFieldsScore = calculateRequiredFieldsScore(demand);
        int descriptionScore = calculateDescriptionScore(demand);
        int acceptanceCriteriaScore = calculateAcceptanceCriteriaScore(demand);
        int technicalDetailsScore = calculateTechnicalDetailsScore(demand);
        int businessValueScore = calculateBusinessValueScore(demand);

        // 2. 计算总分
        int totalScore = requiredFieldsScore + descriptionScore +
                acceptanceCriteriaScore + technicalDetailsScore +
                businessValueScore;

        // 3. 确定评级
        String rating = determineRating(totalScore);

        // 4. 生成评分详情和建议
        Map<String, Object> scoreDetails = generateScoreDetails(
                requiredFieldsScore, descriptionScore, acceptanceCriteriaScore,
                technicalDetailsScore, businessValueScore);

        List<String> suggestions = generateSuggestions(demand, requiredFieldsScore,
                descriptionScore, acceptanceCriteriaScore, technicalDetailsScore,
                businessValueScore);

        // 5. 保存或更新评分
        DemandQualityScore score = scoreMapper.selectById(demandId);
        if (score == null) {
            score = new DemandQualityScore();
            score.setDemandId(demandId);
        }

        score.setTotalScore(totalScore);
        score.setRating(rating);
        score.setRequiredFieldsScore(requiredFieldsScore);
        score.setDescriptionScore(descriptionScore);
        score.setAcceptanceCriteriaScore(acceptanceCriteriaScore);
        score.setTechnicalDetailsScore(technicalDetailsScore);
        score.setBusinessValueScore(businessValueScore);
        score.setScoreTime(LocalDateTime.now());
        score.setNotified(false);

        try {
            score.setScoreDetails(objectMapper.writeValueAsString(scoreDetails));
            score.setSuggestions(objectMapper.writeValueAsString(suggestions));
        } catch (Exception e) {
            log.warn("序列化评分数据失败", e);
        }

        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }

        // 6. 如果是低质量需求，发送提醒
        if (totalScore < LOW_QUALITY_THRESHOLD) {
            sendLowQualityNotification(demand, score, suggestions);
        }

        log.info("需求质量评分完成: demandId={}, totalScore={}, rating={}",
                demandId, totalScore, rating);

        return score;
    }

    /**
     * 计算必填字段得分（0-30分）
     */
    private int calculateRequiredFieldsScore(Demand demand) {
        int score = 0;

        // 标题（10分）
        if (demand.getTitle() != null && !demand.getTitle().trim().isEmpty()) {
            score += 10;
        }

        // 类型（5分）
        if (demand.getType() != null && !demand.getType().trim().isEmpty()) {
            score += 5;
        }

        // 优先级（5分）
        if (demand.getPriority() != null && !demand.getPriority().trim().isEmpty()) {
            score += 5;
        }

        // 项目ID（5分）
        if (demand.getProjectId() != null) {
            score += 5;
        }

        // 模块ID（3分）
        if (demand.getModuleId() != null) {
            score += 3;
        }

        // 预估工时（2分）
        if (demand.getEstimatedHours() != null) {
            score += 2;
        }

        return score;
    }

    /**
     * 计算描述质量得分（0-25分）
     */
    private int calculateDescriptionScore(Demand demand) {
        if (demand.getDescription() == null || demand.getDescription().trim().isEmpty()) {
            return 0;
        }

        String desc = demand.getDescription().trim();
        int length = desc.length();

        // 长度评分（0-15分）
        int lengthScore = 0;
        if (length >= 500) {
            lengthScore = 15;
        } else if (length >= 200) {
            lengthScore = 12;
        } else if (length >= 100) {
            lengthScore = 8;
        } else if (length >= 50) {
            lengthScore = 5;
        } else {
            lengthScore = 2;
        }

        // 结构评分（0-10分）
        int structureScore = 0;
        boolean hasBackground = desc.contains("背景") || desc.contains("背景：");
        boolean hasGoal = desc.contains("目标") || desc.contains("目的");
        boolean hasScope = desc.contains("范围") || desc.contains("涉及");
        boolean hasExample = desc.contains("例如") || desc.contains("示例");

        int structureCount = 0;
        if (hasBackground) structureCount++;
        if (hasGoal) structureCount++;
        if (hasScope) structureCount++;
        if (hasExample) structureCount++;

        structureScore = Math.min(10, structureCount * 3);

        return lengthScore + structureScore;
    }

    /**
     * 计算验收标准得分（0-20分）
     */
    private int calculateAcceptanceCriteriaScore(Demand demand) {
        if (demand.getAcceptanceCriteria() == null || demand.getAcceptanceCriteria().trim().isEmpty()) {
            return 0;
        }

        String criteria = demand.getAcceptanceCriteria().trim();
        int score = 0;

        // 长度评分（0-10分）
        if (criteria.length() >= 200) {
            score += 10;
        } else if (criteria.length() >= 100) {
            score += 7;
        } else if (criteria.length() >= 50) {
            score += 4;
        } else {
            score += 2;
        }

        // 检查是否有清晰的验收点（0-10分）
        int checklistCount = 0;
        if (criteria.contains("- [ ]") || criteria.contains("□")) {
            checklistCount = criteria.split("- \\[ \\]").length - 1;
        } else if (criteria.contains("\n")) {
            checklistCount = criteria.split("\n").length;
        }

        score += Math.min(10, checklistCount * 2);

        return score;
    }

    /**
     * 计算技术细节得分（0-15分）
     */
    private int calculateTechnicalDetailsScore(Demand demand) {
        int score = 0;

        // 技术方案（8分）
        if (demand.getTechnicalSolution() != null && !demand.getTechnicalSolution().trim().isEmpty()) {
            String solution = demand.getTechnicalSolution().trim();
            if (solution.length() >= 100) {
                score += 8;
            } else if (solution.length() >= 50) {
                score += 5;
            } else {
                score += 2;
            }
        }

        // 风险说明（7分）
        if (demand.getRiskDescription() != null && !demand.getRiskDescription().trim().isEmpty()) {
            String risk = demand.getRiskDescription().trim();
            if (risk.length() >= 100) {
                score += 7;
            } else if (risk.length() >= 50) {
                score += 4;
            } else {
                score += 2;
            }
        }

        return score;
    }

    /**
     * 计算业务价值得分（0-10分）
     */
    private int calculateBusinessValueScore(Demand demand) {
        if (demand.getBusinessValue() == null || demand.getBusinessValue().trim().isEmpty()) {
            return 0;
        }

        String value = demand.getBusinessValue().trim();
        if (value.length() >= 100) {
            return 10;
        } else if (value.length() >= 50) {
            return 7;
        } else if (value.length() >= 20) {
            return 4;
        } else {
            return 2;
        }
    }

    /**
     * 确定评级
     */
    private String determineRating(int totalScore) {
        if (totalScore >= 90) {
            return "EXCELLENT";
        } else if (totalScore >= 75) {
            return "GOOD";
        } else if (totalScore >= 60) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    /**
     * 生成评分详情
     */
    private Map<String, Object> generateScoreDetails(int requiredFields, int description,
                                                     int acceptanceCriteria, int technicalDetails,
                                                     int businessValue) {
        Map<String, Object> details = new HashMap<>();
        details.put("requiredFields", Map.of(
                "score", requiredFields,
                "max", REQUIRED_FIELDS_MAX,
                "percentage", Math.round(requiredFields * 100.0 / REQUIRED_FIELDS_MAX)
        ));
        details.put("description", Map.of(
                "score", description,
                "max", DESCRIPTION_MAX,
                "percentage", Math.round(description * 100.0 / DESCRIPTION_MAX)
        ));
        details.put("acceptanceCriteria", Map.of(
                "score", acceptanceCriteria,
                "max", ACCEPTANCE_CRITERIA_MAX,
                "percentage", Math.round(acceptanceCriteria * 100.0 / ACCEPTANCE_CRITERIA_MAX)
        ));
        details.put("technicalDetails", Map.of(
                "score", technicalDetails,
                "max", TECHNICAL_DETAILS_MAX,
                "percentage", Math.round(technicalDetails * 100.0 / TECHNICAL_DETAILS_MAX)
        ));
        details.put("businessValue", Map.of(
                "score", businessValue,
                "max", BUSINESS_VALUE_MAX,
                "percentage", Math.round(businessValue * 100.0 / BUSINESS_VALUE_MAX)
        ));

        return details;
    }

    /**
     * 生成改进建议
     */
    private List<String> generateSuggestions(Demand demand, int requiredFields, int description,
                                             int acceptanceCriteria, int technicalDetails,
                                             int businessValue) {
        List<String> suggestions = new ArrayList<>();

        if (requiredFields < REQUIRED_FIELDS_MAX) {
            suggestions.add("请补充完整的必填字段（标题、类型、优先级、项目等）");
        }

        if (description < DESCRIPTION_MAX * 0.6) {
            suggestions.add("建议详细描述需求背景、目标和范围，至少200字以上");
        }

        if (acceptanceCriteria < ACCEPTANCE_CRITERIA_MAX * 0.5) {
            suggestions.add("请添加清晰的验收标准，使用清单格式列出验收点");
        }

        if (technicalDetails < TECHNICAL_DETAILS_MAX * 0.5) {
            if (demand.getTechnicalSolution() == null || demand.getTechnicalSolution().isEmpty()) {
                suggestions.add("建议补充技术方案概要");
            }
            if (demand.getRiskDescription() == null || demand.getRiskDescription().isEmpty()) {
                suggestions.add("建议补充风险说明");
            }
        }

        if (businessValue < BUSINESS_VALUE_MAX * 0.5) {
            suggestions.add("请说明需求的业务价值和预期收益");
        }

        return suggestions;
    }

    /**
     * 发送低质量需求提醒
     */
    private void sendLowQualityNotification(Demand demand, DemandQualityScore score, List<String> suggestions) {
        try {
            String suggestionText = String.join("；", suggestions);
            String content = String.format(
                    "您的需求【%s】质量评分较低（%d分，%s），建议改进：%s",
                    demand.getTitle(), score.getTotalScore(), getRatingText(score.getRating()),
                    suggestionText
            );

            notificationService.sendNotification(
                    0L,                    // 系统发送
                    demand.getCreatorId(), // 接收人：需求提出者
                    "需求质量改进建议",
                    content,
                    4,                     // 类型：质量提醒
                    demand.getId()
            );

            // 标记为已提醒
            scoreMapper.markAsNotified(demand.getId());

            log.info("已发送低质量需求提醒: demandId={}, score={}", demand.getId(), score.getTotalScore());
        } catch (Exception e) {
            log.error("发送低质量需求提醒失败: demandId={}", demand.getId(), e);
        }
    }

    /**
     * 获取评级文本
     */
    private String getRatingText(String rating) {
        return switch (rating) {
            case "EXCELLENT" -> "优秀";
            case "GOOD" -> "良好";
            case "FAIR" -> "一般";
            case "POOR" -> "较差";
            default -> "未知";
        };
    }

    /**
     * 批量检查所有需求的质量评分
     */
    @Transactional
    public void checkAllDemandsQuality() {
        log.info("开始批量检查需求质量...");

        List<Demand> allDemands = demandMapper.selectAll();
        int checkedCount = 0;

        for (Demand demand : allDemands) {
            try {
                calculateScore(demand.getId());
                checkedCount++;
            } catch (Exception e) {
                log.error("检查需求质量失败: demandId={}", demand.getId(), e);
            }
        }

        log.info("批量检查完成，共检查 {} 个需求", checkedCount);
    }
}
