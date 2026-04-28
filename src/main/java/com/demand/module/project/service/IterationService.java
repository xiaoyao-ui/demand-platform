package com.demand.module.project.service;

import com.demand.exception.BusinessException;
import com.demand.module.project.entity.Iteration;
import com.demand.module.project.mapper.IterationMapper;
import com.demand.module.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 迭代业务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IterationService {

    private final IterationMapper iterationMapper;
    private final ProjectMapper projectMapper;
    private final ProjectPermissionService permissionService;

    public List<Iteration> getIterationsByProjectId(Long projectId, Long currentUserId) {
        permissionService.checkCanViewProject(projectId, currentUserId);
        return iterationMapper.selectIterationsByProjectId(projectId);
    }

    public List<Iteration> getActiveIterations(Long projectId, Long currentUserId) {
        permissionService.checkCanViewProject(projectId, currentUserId);
        return iterationMapper.selectActiveIterations(projectId);
    }

    public Iteration getIterationById(Long id, Long currentUserId) {
        Iteration iteration = iterationMapper.selectById(id);
        if (iteration == null) {
            throw new BusinessException("迭代不存在");
        }
        
        permissionService.checkCanViewProject(iteration.getProjectId(), currentUserId);
        return iteration;
    }

    @Transactional
    public void createIteration(Iteration iteration, Long currentUserId) {
        permissionService.checkCanManageIteration(iteration.getProjectId(), currentUserId);

        var project = projectMapper.selectById(iteration.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        iteration.setCreateTime(LocalDateTime.now());
        iterationMapper.insert(iteration);
        log.info("迭代创建成功: iterationId={}", iteration.getId());
    }

    @Transactional
    public void updateIteration(Long id, Iteration iteration, Long currentUserId) {
        Iteration existIteration = iterationMapper.selectById(id);
        if (existIteration == null) {
            throw new BusinessException("迭代不存在");
        }

        permissionService.checkCanManageIteration(existIteration.getProjectId(), currentUserId);

        iteration.setId(id);
        iterationMapper.updateById(iteration);
        log.info("迭代更新成功: iterationId={}", id);
    }

    @Transactional
    public void deleteIteration(Long id, Long currentUserId) {
        Iteration existIteration = iterationMapper.selectById(id);
        if (existIteration == null) {
            throw new BusinessException("迭代不存在");
        }

        permissionService.checkCanManageIteration(existIteration.getProjectId(), currentUserId);

        iterationMapper.deleteById(id);
        log.info("迭代删除成功: iterationId={}", id);
    }

    @Transactional
    public void startIteration(Long id, Long currentUserId) {
        Iteration iteration = iterationMapper.selectById(id);
        if (iteration == null) {
            throw new BusinessException("迭代不存在");
        }

        permissionService.checkCanManageIteration(iteration.getProjectId(), currentUserId);

        iteration.setStatus(1);
        iteration.setStartTime(LocalDateTime.now());
        iterationMapper.updateById(iteration);
        log.info("迭代开始: iterationId={}", id);
    }

    @Transactional
    public void endIteration(Long id, Long currentUserId) {
        Iteration iteration = iterationMapper.selectById(id);
        if (iteration == null) {
            throw new BusinessException("迭代不存在");
        }

        permissionService.checkCanManageIteration(iteration.getProjectId(), currentUserId);

        iteration.setStatus(2);
        iteration.setEndTime(LocalDateTime.now());
        iterationMapper.updateById(iteration);
        log.info("迭代结束: iterationId={}", id);
    }
}
