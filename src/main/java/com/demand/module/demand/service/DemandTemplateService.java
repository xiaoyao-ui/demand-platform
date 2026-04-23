package com.demand.module.demand.service;

import com.demand.module.demand.entity.DemandTemplate;
import com.demand.module.demand.mapper.DemandTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求模板 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandTemplateService {

    private final DemandTemplateMapper templateMapper;

    /**
     * 查询所有启用的模板
     */
    public List<DemandTemplate> getAllEnabled() {
        return templateMapper.selectAllEnabled();
    }

    /**
     * 查询所有模板（管理用）
     */
    public List<DemandTemplate> getAll() {
        return templateMapper.selectAll();
    }

    /**
     * 根据 ID 查询模板
     */
    public DemandTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    /**
     * 创建模板
     */
    public void create(DemandTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setStatus(template.getStatus() == null ? 1 : template.getStatus());
        templateMapper.insert(template);
    }

    /**
     * 更新模板
     */
    public void update(DemandTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.update(template);
    }

    /**
     * 删除模板
     */
    public void delete(Long id) {
        templateMapper.deleteById(id);
    }
}
