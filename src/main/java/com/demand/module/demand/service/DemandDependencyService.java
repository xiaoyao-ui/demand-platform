package com.demand.module.demand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demand.exception.BusinessException;
import com.demand.module.demand.dto.ImpactAnalysisDTO;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandDependency;
import com.demand.module.demand.mapper.DemandDependencyMapper;
import com.demand.module.demand.mapper.DemandMapper;
import com.demand.module.project.entity.Project;
import com.demand.module.project.mapper.ProjectMapper;
import com.demand.module.user.entity.User;
import com.demand.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandDependencyService {

    private final DemandDependencyMapper dependencyMapper;
    private final DemandMapper demandMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    public List<DemandDependency> getDependenciesByDemandId(Long demandId) {
        return dependencyMapper.selectDependenciesByDemandId(demandId);
    }

    public List<DemandDependency> getDependentDemands(Long demandId) {
        return dependencyMapper.selectDependentDemands(demandId);
    }

    @Transactional
    public void addDependency(Long demandId, Long dependsOnId, String dependencyType) {
        if (demandId.equals(dependsOnId)) {
            throw new BusinessException("不能依赖自身");
        }

        Demand existDemand = demandMapper.selectById(dependsOnId);
        if (existDemand == null) {
            throw new BusinessException("依赖的需求不存在");
        }

        DemandDependency existDependency = dependencyMapper.selectOne(new LambdaQueryWrapper<DemandDependency>()
                .eq(DemandDependency::getDemandId, demandId)
                .eq(DemandDependency::getDependsOnId, dependsOnId));

        if (existDependency != null) {
            throw new BusinessException("依赖关系已存在");
        }

        if (hasCircularDependency(demandId, dependsOnId)) {
            throw new BusinessException("检测到循环依赖");
        }

        DemandDependency dependency = new DemandDependency();
        dependency.setDemandId(demandId);
        dependency.setDependsOnId(dependsOnId);
        dependency.setDependencyType(dependencyType != null ? dependencyType : "BLOCKS");
        dependencyMapper.insert(dependency);

        log.info("添加依赖关系: demandId={}, dependsOnId={}, type={}",
                demandId, dependsOnId, dependencyType);
    }

    @Transactional
    public void removeDependency(Long demandId, Long dependsOnId) {
        dependencyMapper.delete(new LambdaQueryWrapper<DemandDependency>()
                .eq(DemandDependency::getDemandId, demandId)
                .eq(DemandDependency::getDependsOnId, dependsOnId));

        log.info("移除依赖关系: demandId={}, dependsOnId={}", demandId, dependsOnId);
    }

    private boolean hasCircularDependency(Long demandId, Long dependsOnId) {
        List<DemandDependency> dependencies = dependencyMapper.selectDependenciesByDemandId(dependsOnId);
        for (DemandDependency dep : dependencies) {
            if (dep.getDependsOnId().equals(demandId)) {
                return true;
            }
            if (hasCircularDependency(demandId, dep.getDependsOnId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 分析需求变更的影响范围
     *
     * @param demandId 需求 ID
     * @param changeType 变更类型
     * @return 影响分析结果
     */
    public ImpactAnalysisDTO analyzeImpact(Long demandId, String changeType) {
        log.info("分析需求变更影响: demandId={}, changeType={}", demandId, changeType);
        
        Demand demand = demandMapper.findById(demandId);
        if (demand == null) {
            throw new BusinessException("需求不存在");
        }
        
        ImpactAnalysisDTO analysis = new ImpactAnalysisDTO();
        analysis.setDemandId(demandId);
        analysis.setDemandTitle(demand.getTitle());
        analysis.setChangeType(changeType);
        
        // 1. 查找直接影响（一级依赖）
        List<ImpactAnalysisDTO.ImpactedDemand> directImpacts = findDirectImpacts(demandId);
        analysis.setDirectImpacts(directImpacts);
        
        // 2. 查找间接影响（二级依赖）
        List<ImpactAnalysisDTO.ImpactedDemand> indirectImpacts = findIndirectImpacts(demandId, directImpacts);
        analysis.setIndirectImpacts(indirectImpacts);
        
        // 3. 统计影响的项目
        List<ImpactAnalysisDTO.ImpactedProject> impactedProjects = calculateImpactedProjects(
                directImpacts, indirectImpacts);
        analysis.setImpactedProjects(impactedProjects);
        
        // 4. 统计影响的模块
        List<ImpactAnalysisDTO.ImpactedModule> impactedModules = calculateImpactedModules(
                directImpacts, indirectImpacts);
        analysis.setImpactedModules(impactedModules);
        
        // 5. 统计影响的人员
        List<ImpactAnalysisDTO.ImpactedPerson> impactedPersons = calculateImpactedPersons(
                directImpacts, indirectImpacts);
        analysis.setImpactedPersons(impactedPersons);
        
        // 6. 构建影响链路图
        ImpactAnalysisDTO.ImpactGraph graph = buildImpactGraph(demand, directImpacts, indirectImpacts);
        analysis.setGraph(graph);
        
        // 7. 计算影响等级和评分
        int impactScore = calculateImpactScore(directImpacts.size(), indirectImpacts.size());
        analysis.setImpactScore(impactScore);
        analysis.setImpactLevel(determineImpactLevel(impactScore));
        
        // 8. 风险评估
        ImpactAnalysisDTO.RiskAssessment riskAssessment = assessRisk(
                directImpacts, indirectImpacts, impactedProjects, impactedPersons);
        analysis.setRiskAssessment(riskAssessment);
        
        // 9. 生成建议措施
        List<String> suggestions = generateSuggestions(analysis);
        analysis.setSuggestions(suggestions);
        
        log.info("影响分析完成: demandId={}, impactLevel={}, totalImpacted={}", 
                demandId, analysis.getImpactLevel(), 
                directImpacts.size() + indirectImpacts.size());
        
        return analysis;
    }

    /**
     * 查找直接影响（一级依赖）
     */
    private List<ImpactAnalysisDTO.ImpactedDemand> findDirectImpacts(Long demandId) {
        List<ImpactAnalysisDTO.ImpactedDemand> impacts = new ArrayList<>();
        
        // 查找依赖于当前需求的需求（当前需求被阻塞）
        List<DemandDependency> blockers = dependencyMapper.selectDependentDemands(demandId);
        for (DemandDependency dep : blockers) {
            Demand dependentDemand = demandMapper.findById(dep.getDemandId());
            if (dependentDemand != null) {
                ImpactAnalysisDTO.ImpactedDemand impact = convertToImpactedDemand(
                        dependentDemand, "BLOCKED_BY", 1, 80);
                impact.setImpactDescription(String.format("该需求依赖于当前需求，当前需求的变更将直接影响其开发进度"));
                impacts.add(impact);
            }
        }
        
        // 查找当前需求依赖的其他需求（可能需要同步调整）
        List<DemandDependency> dependencies = dependencyMapper.selectDependenciesByDemandId(demandId);
        for (DemandDependency dep : dependencies) {
            Demand dependedDemand = demandMapper.findById(dep.getDependsOnId());
            if (dependedDemand != null && !impacts.stream().anyMatch(i -> i.getId().equals(dependedDemand.getId()))) {
                ImpactAnalysisDTO.ImpactedDemand impact = convertToImpactedDemand(
                        dependedDemand, "DEPENDS_ON", 1, 60);
                impact.setImpactDescription(String.format("当前需求依赖于此需求，可能需要重新评估依赖关系"));
                impacts.add(impact);
            }
        }
        
        return impacts;
    }

    /**
     * 查找间接影响（二级依赖）
     */
    private List<ImpactAnalysisDTO.ImpactedDemand> findIndirectImpacts(
            Long demandId, List<ImpactAnalysisDTO.ImpactedDemand> directImpacts) {
        Set<Long> directIds = directImpacts.stream()
                .map(ImpactAnalysisDTO.ImpactedDemand::getId)
                .collect(Collectors.toSet());
        directIds.add(demandId); // 排除已处理的需求
        
        List<ImpactAnalysisDTO.ImpactedDemand> indirectImpacts = new ArrayList<>();
        
        for (ImpactAnalysisDTO.ImpactedDemand direct : directImpacts) {
            // 查找二级依赖
            List<DemandDependency> secondLevel = dependencyMapper.selectDependentDemands(direct.getId());
            for (DemandDependency dep : secondLevel) {
                if (!directIds.contains(dep.getDemandId())) {
                    Demand secondDemand = demandMapper.findById(dep.getDemandId());
                    if (secondDemand != null) {
                        ImpactAnalysisDTO.ImpactedDemand impact = convertToImpactedDemand(
                                secondDemand, "INDIRECT", 2, 40);
                        impact.setImpactDescription(String.format("通过【%s】间接受到影响", direct.getTitle()));
                        indirectImpacts.add(impact);
                        directIds.add(dep.getDemandId());
                    }
                }
            }
        }
        
        return indirectImpacts;
    }

    /**
     * 计算影响的项目
     */
    private List<ImpactAnalysisDTO.ImpactedProject> calculateImpactedProjects(
            List<ImpactAnalysisDTO.ImpactedDemand> direct, 
            List<ImpactAnalysisDTO.ImpactedDemand> indirect) {
        Map<Long, ImpactAnalysisDTO.ImpactedProject> projectMap = new HashMap<>();
        
        for (ImpactAnalysisDTO.ImpactedDemand impact : direct) {
            aggregateProject(projectMap, impact, true);
        }
        for (ImpactAnalysisDTO.ImpactedDemand impact : indirect) {
            aggregateProject(projectMap, impact, false);
        }
        
        return new ArrayList<>(projectMap.values());
    }

    /**
     * 聚合项目信息
     */
    private void aggregateProject(Map<Long, ImpactAnalysisDTO.ImpactedProject> map, 
                                   ImpactAnalysisDTO.ImpactedDemand impact, boolean isDirect) {
        Demand demand = demandMapper.findById(impact.getId());
        if (demand == null || demand.getProjectId() == null) return;
        
        Long projectId = demand.getProjectId();
        ImpactAnalysisDTO.ImpactedProject project = map.get(projectId);
        if (project == null) {
            Project p = projectMapper.selectById(projectId);
            project = new ImpactAnalysisDTO.ImpactedProject();
            project.setProjectId(projectId);
            project.setProjectName(p != null ? p.getName() : "未知项目");
            project.setAffectedDemandCount(0);
            map.put(projectId, project);
        }
        project.setAffectedDemandCount(project.getAffectedDemandCount() + 1);
    }

    /**
     * 计算影响的模块
     */
    private List<ImpactAnalysisDTO.ImpactedModule> calculateImpactedModules(
            List<ImpactAnalysisDTO.ImpactedDemand> direct, 
            List<ImpactAnalysisDTO.ImpactedDemand> indirect) {
        Map<Long, ImpactAnalysisDTO.ImpactedModule> moduleMap = new HashMap<>();
        
        for (ImpactAnalysisDTO.ImpactedDemand impact : direct) {
            aggregateModule(moduleMap, impact);
        }
        for (ImpactAnalysisDTO.ImpactedDemand impact : indirect) {
            aggregateModule(moduleMap, impact);
        }
        
        return new ArrayList<>(moduleMap.values());
    }

    /**
     * 聚合模块信息
     */
    private void aggregateModule(Map<Long, ImpactAnalysisDTO.ImpactedModule> map, 
                                  ImpactAnalysisDTO.ImpactedDemand impact) {
        Demand demand = demandMapper.findById(impact.getId());
        if (demand == null || demand.getModuleId() == null) return;
        
        Long moduleId = demand.getModuleId();
        ImpactAnalysisDTO.ImpactedModule module = map.get(moduleId);
        if (module == null) {
            module = new ImpactAnalysisDTO.ImpactedModule();
            module.setModuleId(moduleId);
            module.setModuleName("模块-" + moduleId); // 简化处理
            module.setAffectedDemandCount(0);
            map.put(moduleId, module);
        }
        module.setAffectedDemandCount(module.getAffectedDemandCount() + 1);
    }

    /**
     * 计算影响的人员
     */
    private List<ImpactAnalysisDTO.ImpactedPerson> calculateImpactedPersons(
            List<ImpactAnalysisDTO.ImpactedDemand> direct, 
            List<ImpactAnalysisDTO.ImpactedDemand> indirect) {
        Map<Long, ImpactAnalysisDTO.ImpactedPerson> personMap = new HashMap<>();
        
        for (ImpactAnalysisDTO.ImpactedDemand impact : direct) {
            aggregatePerson(personMap, impact, true);
        }
        for (ImpactAnalysisDTO.ImpactedDemand impact : indirect) {
            aggregatePerson(personMap, impact, false);
        }
        
        return new ArrayList<>(personMap.values());
    }

    /**
     * 聚合人员信息
     */
    private void aggregatePerson(Map<Long, ImpactAnalysisDTO.ImpactedPerson> map, 
                                  ImpactAnalysisDTO.ImpactedDemand impact, boolean isDirect) {
        Demand demand = demandMapper.findById(impact.getId());
        if (demand == null || demand.getAssigneeId() == null) return;
        
        Long userId = demand.getAssigneeId();
        ImpactAnalysisDTO.ImpactedPerson person = map.get(userId);
        if (person == null) {
            User user = userMapper.findById(userId);
            person = new ImpactAnalysisDTO.ImpactedPerson();
            person.setUserId(userId);
            person.setUserName(user != null ? user.getRealName() : "未知");
            person.setRole("开发人员");
            person.setDemandCount(0);
            map.put(userId, person);
        }
        person.setDemandCount(person.getDemandCount() + 1);
    }

    /**
     * 转换为受影响需求对象
     */
    private ImpactAnalysisDTO.ImpactedDemand convertToImpactedDemand(
            Demand demand, String dependencyType, int level, int baseDegree) {
        ImpactAnalysisDTO.ImpactedDemand impact = new ImpactAnalysisDTO.ImpactedDemand();
        impact.setId(demand.getId());
        impact.setTitle(demand.getTitle());
        impact.setStatus(demand.getStatus());
        impact.setPriority(demand.getPriority());
        impact.setDependencyType(dependencyType);
        impact.setLevel(level);
        impact.setImpactDegree(baseDegree);
        
        // 获取负责人
        if (demand.getAssigneeId() != null) {
            User assignee = userMapper.findById(demand.getAssigneeId());
            impact.setAssigneeName(assignee != null ? assignee.getRealName() : null);
        }
        
        // 获取项目
        if (demand.getProjectId() != null) {
            Project project = projectMapper.selectById(demand.getProjectId());
            impact.setProjectName(project != null ? project.getName() : null);
        }
        
        return impact;
    }

    /**
     * 构建影响链路图
     */
    private ImpactAnalysisDTO.ImpactGraph buildImpactGraph(
            Demand sourceDemand, 
            List<ImpactAnalysisDTO.ImpactedDemand> direct, 
            List<ImpactAnalysisDTO.ImpactedDemand> indirect) {
        ImpactAnalysisDTO.ImpactGraph graph = new ImpactAnalysisDTO.ImpactGraph();
        List<ImpactAnalysisDTO.ImpactGraph.GraphNode> nodes = new ArrayList<>();
        List<ImpactAnalysisDTO.ImpactGraph.GraphEdge> edges = new ArrayList<>();
        
        // 添加源节点
        ImpactAnalysisDTO.ImpactGraph.GraphNode sourceNode = new ImpactAnalysisDTO.ImpactGraph.GraphNode();
        sourceNode.setId("demand_" + sourceDemand.getId());
        sourceNode.setLabel(sourceDemand.getTitle());
        sourceNode.setType("demand");
        sourceNode.setSize(30);
        sourceNode.setColor("#FF0000"); // 红色表示源节点
        nodes.add(sourceNode);
        
        // 添加直接影响节点
        for (ImpactAnalysisDTO.ImpactedDemand impact : direct) {
            ImpactAnalysisDTO.ImpactGraph.GraphNode node = new ImpactAnalysisDTO.ImpactGraph.GraphNode();
            node.setId("demand_" + impact.getId());
            node.setLabel(impact.getTitle());
            node.setType("demand");
            node.setSize(25);
            node.setColor("#FFA500"); // 橙色表示直接影响
            nodes.add(node);
            
            // 添加边
            ImpactAnalysisDTO.ImpactGraph.GraphEdge edge = new ImpactAnalysisDTO.ImpactGraph.GraphEdge();
            edge.setSource(sourceNode.getId());
            edge.setTarget(node.getId());
            edge.setLabel(impact.getDependencyType());
            edge.setEdgeType("direct");
            edges.add(edge);
        }
        
        // 添加间接影响节点
        for (ImpactAnalysisDTO.ImpactedDemand impact : indirect) {
            ImpactAnalysisDTO.ImpactGraph.GraphNode node = new ImpactAnalysisDTO.ImpactGraph.GraphNode();
            node.setId("demand_" + impact.getId());
            node.setLabel(impact.getTitle());
            node.setType("demand");
            node.setSize(20);
            node.setColor("#FFFF00"); // 黄色表示间接影响
            nodes.add(node);
            
            // 找到对应的直接节点作为源
            String sourceId = findParentNodeId(impact, direct);
            if (sourceId != null) {
                ImpactAnalysisDTO.ImpactGraph.GraphEdge edge = new ImpactAnalysisDTO.ImpactGraph.GraphEdge();
                edge.setSource(sourceId);
                edge.setTarget(node.getId());
                edge.setLabel("间接影响");
                edge.setEdgeType("indirect");
                edges.add(edge);
            }
        }
        
        graph.setNodes(nodes);
        graph.setEdges(edges);
        
        return graph;
    }

    /**
     * 查找父节点ID
     */
    private String findParentNodeId(ImpactAnalysisDTO.ImpactedDemand impact, 
                                     List<ImpactAnalysisDTO.ImpactedDemand> direct) {
        // 简化处理：返回第一个直接节点
        if (!direct.isEmpty()) {
            return "demand_" + direct.get(0).getId();
        }
        return null;
    }

    /**
     * 计算影响评分
     */
    private int calculateImpactScore(int directCount, int indirectCount) {
        int score = directCount * 20 + indirectCount * 5;
        return Math.min(score, 100);
    }

    /**
     * 确定影响等级
     */
    private String determineImpactLevel(int score) {
        if (score >= 70) {
            return "HIGH";
        } else if (score >= 40) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 风险评估
     */
    private ImpactAnalysisDTO.RiskAssessment assessRisk(
            List<ImpactAnalysisDTO.ImpactedDemand> direct,
            List<ImpactAnalysisDTO.ImpactedDemand> indirect,
            List<ImpactAnalysisDTO.ImpactedProject> projects,
            List<ImpactAnalysisDTO.ImpactedPerson> persons) {
        
        ImpactAnalysisDTO.RiskAssessment assessment = new ImpactAnalysisDTO.RiskAssessment();
        
        int totalImpacted = direct.size() + indirect.size();
        assessment.setTotalImpactedDemands(totalImpacted);
        assessment.setImpactedProjectCount(projects.size());
        assessment.setImpactedPersonCount(persons.size());
        
        // 计算风险分数
        int riskScore = totalImpacted * 15 + projects.size() * 10 + persons.size() * 5;
        riskScore = Math.min(riskScore, 100);
        assessment.setRiskScore(riskScore);
        
        // 确定风险等级
        if (riskScore >= 70) {
            assessment.setRiskLevel("HIGH");
        } else if (riskScore >= 40) {
            assessment.setRiskLevel("MEDIUM");
        } else {
            assessment.setRiskLevel("LOW");
        }
        
        // 识别高风险因素
        List<String> highRiskFactors = new ArrayList<>();
        if (totalImpacted > 5) {
            highRiskFactors.add("影响需求数量较多（" + totalImpacted + "个）");
        }
        if (projects.size() > 2) {
            highRiskFactors.add("跨多个项目（" + projects.size() + "个）");
        }
        if (persons.size() > 3) {
            highRiskFactors.add("影响多人协作（" + persons.size() + "人）");
        }
        
        long inProgressCount = direct.stream()
                .filter(d -> "IN_DEVELOPMENT".equals(d.getStatus()) || "IN_TEST".equals(d.getStatus()))
                .count();
        if (inProgressCount > 0) {
            highRiskFactors.add("有" + inProgressCount + "个需求正在开发/测试中");
        }
        
        assessment.setHighRiskFactors(highRiskFactors);
        
        // 生成风险说明
        StringBuilder desc = new StringBuilder();
        desc.append("本次变更将影响 ").append(totalImpacted).append(" 个需求");
        if (!projects.isEmpty()) {
            desc.append("，涉及 ").append(projects.size()).append(" 个项目");
        }
        if (!persons.isEmpty()) {
            desc.append("，影响 ").append(persons.size()).append(" 位开发人员");
        }
        assessment.setRiskDescription(desc.toString());
        
        return assessment;
    }

    /**
     * 生成建议措施
     */
    private List<String> generateSuggestions(ImpactAnalysisDTO analysis) {
        List<String> suggestions = new ArrayList<>();
        
        if ("HIGH".equals(analysis.getImpactLevel())) {
            suggestions.add("⚠️ 高风险变更，建议在变更前召开评审会议");
            suggestions.add("通知所有受影响的负责人，协调变更计划");
            suggestions.add("制定详细的回滚方案");
        }
        
        if (analysis.getRiskAssessment().getImpactedProjectCount() > 1) {
            suggestions.add("跨项目变更，需要各项目负责人协同配合");
        }
        
        if (analysis.getDirectImpacts().size() > 3) {
            suggestions.add("直接影响较多，建议分阶段实施变更");
        }
        
        long inProgressCount = analysis.getDirectImpacts().stream()
                .filter(d -> "IN_DEVELOPMENT".equals(d.getStatus()))
                .count();
        if (inProgressCount > 0) {
            suggestions.add("有需求正在开发中，请及时同步变更信息给开发人员");
        }
        
        suggestions.add("更新相关文档（PRD、设计稿等）");
        suggestions.add("在变更完成后进行回归测试");
        
        return suggestions;
    }
}
