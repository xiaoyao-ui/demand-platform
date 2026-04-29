package com.demand.module.demand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demand.exception.BusinessException;
import com.demand.module.demand.entity.Demand;
import com.demand.module.demand.entity.DemandDependency;
import com.demand.module.demand.mapper.DemandDependencyMapper;
import com.demand.module.demand.mapper.DemandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandDependencyService {

    private final DemandDependencyMapper dependencyMapper;
    private final DemandMapper demandMapper;

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
}
