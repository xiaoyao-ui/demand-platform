package com.demand.module.demand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 需求影响分析 DTO
 */
@Data
@Schema(description = "需求影响分析结果")
public class ImpactAnalysisDTO {

    @Schema(description = "需求ID")
    private Long demandId;

    @Schema(description = "需求标题")
    private String demandTitle;

    @Schema(description = "变更类型：STATUS-状态变更, CONTENT-内容变更, PRIORITY-优先级变更")
    private String changeType;

    @Schema(description = "影响等级：HIGH-高, MEDIUM-中, LOW-低")
    private String impactLevel;

    @Schema(description = "影响总分")
    private Integer impactScore;

    @Schema(description = "直接影响的需求列表")
    private List<ImpactedDemand> directImpacts;

    @Schema(description = "间接影响的需求列表（二级依赖）")
    private List<ImpactedDemand> indirectImpacts;

    @Schema(description = "影响的项目列表")
    private List<ImpactedProject> impactedProjects;

    @Schema(description = "影响的模块列表")
    private List<ImpactedModule> impactedModules;

    @Schema(description = "影响的负责人列表")
    private List<ImpactedPerson> impactedPersons;

    @Schema(description = "影响链路图数据（用于前端可视化）")
    private ImpactGraph graph;

    @Schema(description = "风险评估")
    private RiskAssessment riskAssessment;

    @Schema(description = "建议措施")
    private List<String> suggestions;

    /**
     * 受影响的需求
     */
    @Data
    @Schema(description = "受影响的需求")
    public static class ImpactedDemand {
        @Schema(description = "需求ID")
        private Long id;

        @Schema(description = "需求标题")
        private String title;

        @Schema(description = "当前状态")
        private String status;

        @Schema(description = "优先级")
        private String priority;

        @Schema(description = "负责人")
        private String assigneeName;

        @Schema(description = "所属项目")
        private String projectName;

        @Schema(description = "依赖类型：DEPENDS_ON-依赖于我, BLOCKED_BY-阻塞我")
        private String dependencyType;

        @Schema(description = "影响程度：0-100")
        private Integer impactDegree;

        @Schema(description = "影响说明")
        private String impactDescription;

        @Schema(description = "层级（1-直接，2-间接）")
        private Integer level;
    }

    /**
     * 影响的项目
     */
    @Data
    @Schema(description = "影响的项目")
    public static class ImpactedProject {
        @Schema(description = "项目ID")
        private Long projectId;

        @Schema(description = "项目名称")
        private String projectName;

        @Schema(description = "受影响需求数")
        private Integer affectedDemandCount;

        @Schema(description = "影响说明")
        private String impactDescription;
    }

    /**
     * 影响的模块
     */
    @Data
    @Schema(description = "影响的模块")
    public static class ImpactedModule {
        @Schema(description = "模块ID")
        private Long moduleId;

        @Schema(description = "模块名称")
        private String moduleName;

        @Schema(description = "受影响需求数")
        private Integer affectedDemandCount;
    }

    /**
     * 影响的人员
     */
    @Data
    @Schema(description = "影响的人员")
    public static class ImpactedPerson {
        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "姓名")
        private String userName;

        @Schema(description = "角色")
        private String role;

        @Schema(description = "负责的需求数")
        private Integer demandCount;

        @Schema(description = "影响说明")
        private String impactDescription;
    }

    /**
     * 影响链路图数据
     */
    @Data
    @Schema(description = "影响链路图数据")
    public static class ImpactGraph {
        @Schema(description = "节点列表")
        private List<GraphNode> nodes;

        @Schema(description = "边列表（依赖关系）")
        private List<GraphEdge> edges;

        @Data
        @Schema(description = "图节点")
        public static class GraphNode {
            @Schema(description = "节点ID")
            private String id;

            @Schema(description = "节点标签")
            private String label;

            @Schema(description = "节点类型：demand-需求, project-项目")
            private String type;

            @Schema(description = "节点大小（基于影响程度）")
            private Integer size;

            @Schema(description = "节点颜色")
            private String color;
        }

        @Data
        @Schema(description = "图边")
        public static class GraphEdge {
            @Schema(description = "源节点ID")
            private String source;

            @Schema(description = "目标节点ID")
            private String target;

            @Schema(description = "边标签")
            private String label;

            @Schema(description = "边类型")
            private String edgeType;
        }
    }

    /**
     * 风险评估
     */
    @Data
    @Schema(description = "风险评估")
    public static class RiskAssessment {
        @Schema(description = "风险等级：HIGH-高, MEDIUM-中, LOW-低")
        private String riskLevel;

        @Schema(description = "风险分数（0-100）")
        private Integer riskScore;

        @Schema(description = "影响的需求总数")
        private Integer totalImpactedDemands;

        @Schema(description = "影响的项目数")
        private Integer impactedProjectCount;

        @Schema(description = "影响的人员数")
        private Integer impactedPersonCount;

        @Schema(description = "高风险因素列表")
        private List<String> highRiskFactors;

        @Schema(description = "风险说明")
        private String riskDescription;
    }
}
