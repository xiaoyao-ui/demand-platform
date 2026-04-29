package com.demand.module.demand.service;

import com.demand.module.demand.entity.DemandTemplate;
import com.demand.module.demand.mapper.DemandTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求模板业务服务
 * <p>
 * 负责管理需求模板的增删改查操作。
 * 模板用于快速创建标准化的需求，提高需求录入效率。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>模板查询</b>：查询所有启用的模板或全部模板（管理用）</li>
 *   <li><b>模板创建</b>：创建新的需求模板</li>
 *   <li><b>模板更新</b>：修改现有模板的内容</li>
 *   <li><b>模板删除</b>：删除不再使用的模板</li>
 * </ul>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>用户创建需求时选择模板，快速填充标准内容</li>
 *   <li>管理员维护常用需求模板库</li>
 *   <li>团队统一需求文档格式和规范</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandTemplateService {

    private final DemandTemplateMapper templateMapper;

    /**
     * 查询所有启用的模板
     * <p>
     * 返回状态为启用（status=1）的模板列表，供用户创建需求时选择。
     * </p>
     *
     * @return 启用的模板列表
     */
    public List<DemandTemplate> getAllEnabled() {
        log.debug("查询所有启用的需求模板");
        return templateMapper.selectAllEnabled();
    }

    /**
     * 查询所有模板（管理用）
     * <p>
     * 返回所有模板（包括启用和禁用），供管理员在后台管理界面使用。
     * </p>
     *
     * @return 全部模板列表
     */
    public List<DemandTemplate> getAll() {
        log.debug("查询所有需求模板（含禁用）");
        return templateMapper.selectAll();
    }

    /**
     * 根据 ID 查询模板
     *
     * @param id 模板 ID
     * @return 模板对象，如果不存在则返回 null
     */
    public DemandTemplate getById(Long id) {
        log.debug("查询需求模板: templateId={}", id);
        return templateMapper.selectById(id);
    }

    /**
     * 创建模板
     * <p>
     * 创建新的需求模板，自动设置创建时间和更新时间。
     * 如果未指定状态，默认为启用（status=1）。
     * </p>
     *
     * @param template 模板对象
     */
    public void create(DemandTemplate template) {
        LocalDateTime now = LocalDateTime.now();
        template.setCreateTime(now);
        template.setUpdateTime(now);
        
        // 默认设置为启用状态
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        
        templateMapper.insert(template);
        log.info("创建需求模板成功: templateId={}, name={}", template.getId(), template.getName());
    }

    /**
     * 更新模板
     * <p>
     * 修改现有模板的内容，自动更新修改时间。
     * </p>
     *
     * @param template 模板对象（必须包含 id）
     */
    public void update(DemandTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.update(template);
        log.info("更新需求模板成功: templateId={}", template.getId());
    }

    /**
     * 删除模板
     * <p>
     * 从数据库中删除指定的模板。
     * 注意：删除前建议检查是否有需求正在使用该模板。
     * </p>
     *
     * @param id 模板 ID
     */
    public void delete(Long id) {
        templateMapper.deleteById(id);
        log.info("删除需求模板成功: templateId={}", id);
    }
}
