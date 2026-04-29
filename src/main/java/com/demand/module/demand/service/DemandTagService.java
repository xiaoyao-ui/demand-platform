package com.demand.module.demand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demand.exception.BusinessException;
import com.demand.module.demand.entity.DemandTag;
import com.demand.module.demand.entity.DemandTagRelation;
import com.demand.module.demand.mapper.DemandTagMapper;
import com.demand.module.demand.mapper.DemandTagRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 需求标签业务服务
 * <p>
 * 负责管理需求的标签系统，包括标签的创建、修改、删除以及与需求的关联。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandTagService {

    private final DemandTagMapper demandTagMapper;
    private final DemandTagRelationMapper demandTagRelationMapper;

    /**
     * 获取所有标签（按使用次数降序）
     *
     * @return 标签列表
     */
    public List<DemandTag> getAllTags() {
        return demandTagMapper.selectList(new LambdaQueryWrapper<DemandTag>()
                .orderByDesc(DemandTag::getUseCount));
    }

    /**
     * 根据分类获取标签
     *
     * @param category 标签分类（TECHNICAL、BUSINESS、PRIORITY、OTHER）
     * @return 标签列表
     */
    public List<DemandTag> getTagsByCategory(String category) {
        return demandTagMapper.selectList(new LambdaQueryWrapper<DemandTag>()
                .eq(DemandTag::getCategory, category)
                .orderByDesc(DemandTag::getUseCount));
    }

    /**
     * 获取需求关联的标签
     *
     * @param demandId 需求 ID
     * @return 标签列表
     */
    public List<DemandTag> getTagsByDemandId(Long demandId) {
        return demandTagMapper.selectTagsByDemandId(demandId);
    }

    /**
     * 创建标签
     *
     * @param tag 标签对象
     */
    @Transactional
    public void createTag(DemandTag tag) {
        if (tag.getColor() == null || tag.getColor().isEmpty()) {
            tag.setColor("#409EFF");
        }
        if (tag.getCategory() == null || tag.getCategory().isEmpty()) {
            tag.setCategory("OTHER");
        }
        tag.setIsSystem(0);
        tag.setUseCount(0);
        demandTagMapper.insert(tag);
        log.info("创建标签成功: tagId={}, name={}", tag.getId(), tag.getName());
    }

    /**
     * 更新标签
     *
     * @param id  标签 ID
     * @param tag 标签对象
     * @throws BusinessException 当标签不存在或是系统内置标签时抛出
     */
    @Transactional
    public void updateTag(Long id, DemandTag tag) {
        DemandTag existTag = demandTagMapper.selectById(id);
        if (existTag == null) {
            throw new BusinessException("标签不存在");
        }

        if (existTag.getIsSystem() == 1) {
            throw new BusinessException("系统内置标签不可修改");
        }

        tag.setId(id);
        demandTagMapper.updateById(tag);
        log.info("更新标签成功: tagId={}", id);
    }

    /**
     * 删除标签
     *
     * @param id 标签 ID
     * @throws BusinessException 当标签不存在或是系统内置标签时抛出
     */
    @Transactional
    public void deleteTag(Long id) {
        DemandTag existTag = demandTagMapper.selectById(id);
        if (existTag == null) {
            throw new BusinessException("标签不存在");
        }

        if (existTag.getIsSystem() == 1) {
            throw new BusinessException("系统内置标签不可删除");
        }

        demandTagMapper.deleteById(id);
        log.info("删除标签成功: tagId={}", id);
    }

    /**
     * 为需求添加标签
     *
     * @param demandId 需求 ID
     * @param tagIds   标签 ID 列表
     */
    @Transactional
    public void addTagsToDemand(Long demandId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        for (Long tagId : tagIds) {
            DemandTagRelation relation = new DemandTagRelation();
            relation.setDemandId(demandId);
            relation.setTagId(tagId);
            demandTagRelationMapper.insert(relation);
            demandTagMapper.incrementUseCount(tagId);
        }

        log.info("为需求添加标签: demandId={}, tagCount={}", demandId, tagIds.size());
    }

    /**
     * 从需求移除标签
     *
     * @param demandId 需求 ID
     * @param tagIds   标签 ID 列表
     */
    @Transactional
    public void removeTagsFromDemand(Long demandId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        for (Long tagId : tagIds) {
            demandTagRelationMapper.delete(new LambdaQueryWrapper<DemandTagRelation>()
                    .eq(DemandTagRelation::getDemandId, demandId)
                    .eq(DemandTagRelation::getTagId, tagId));
            demandTagMapper.decrementUseCount(tagId);
        }

        log.info("从需求移除标签: demandId={}, tagCount={}", demandId, tagIds.size());
    }

    /**
     * 更新需求的标签（先删除旧标签，再添加新标签）
     *
     * @param demandId  需求 ID
     * @param newTagIds 新的标签 ID 列表
     */
    @Transactional
    public void updateDemandTags(Long demandId, List<Long> newTagIds) {
        demandTagRelationMapper.deleteByDemandId(demandId);

        if (newTagIds != null && !newTagIds.isEmpty()) {
            List<DemandTagRelation> relations = newTagIds.stream()
                    .map(tagId -> {
                        DemandTagRelation relation = new DemandTagRelation();
                        relation.setDemandId(demandId);
                        relation.setTagId(tagId);
                        return relation;
                    })
                    .collect(Collectors.toList());

            demandTagRelationMapper.batchInsert(relations);

            for (Long tagId : newTagIds) {
                demandTagMapper.incrementUseCount(tagId);
            }
        }

        log.info("更新需求标签: demandId={}, newTagCount={}", demandId,
                newTagIds == null ? 0 : newTagIds.size());
    }
}
