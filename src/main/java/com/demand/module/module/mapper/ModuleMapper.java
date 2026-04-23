package com.demand.module.module.mapper;

import com.demand.module.module.entity.Module;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 模块数据访问层（Mapper）
 * <p>
 * 提供模块数据的增删查改功能。
 * SQL 语句定义在 {@code src/main/resources/mapper/ModuleMapper.xml} 中。
 * </p>
 * 
 * <h3>核心方法：</h3>
 * <ul>
 *   <li>{@link #findAllEnabled} - 查询所有启用的模块（带缓存）</li>
 *   <li>{@link #findByCode} - 根据编码查询模块（用于唯一性校验）</li>
 *   <li>{@link #insert/update/deleteById} - 增删改操作</li>
 * </ul>
 */
@Mapper
public interface ModuleMapper {

    /**
     * 查询所有启用的模块
     * <p>
     * 只返回状态为启用（status=1）的模块，并按排序号升序排列。
     * 此方法被 {@link com.demand.module.module.service.ModuleService#listEnabledModules} 调用，
     * 并使用 Spring Cache 缓存结果（Key: {@code modules::enabled}）。
     * </p>
     * 
     * <p>
     * <b>SQL 示例</b>：
     * <pre>{@code
     * SELECT * FROM module WHERE status = 1 ORDER BY sort ASC
     * }</pre>
     * </p>
     *
     * @return 启用的模块列表
     */
    List<Module> findAllEnabled();

    /**
     * 查询所有模块（含禁用）
     * <p>
     * 返回系统中的所有模块记录，不分状态。
     * 主要用于后台管理界面展示完整的模块列表。
     * </p>
     *
     * @return 所有模块列表
     */
    List<Module> findAll();

    /**
     * 根据 ID 查询模块
     * <p>
     * 用于更新、删除操作前的存在性检查。
     * </p>
     *
     * @param id 模块 ID
     * @return 模块对象，如果不存在则返回 null
     */
    Module findById(@Param("id") Long id);

    /**
     * 根据编码查询模块
     * <p>
     * 用于创建和更新模块时的唯一性校验，防止编码重复。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>创建模块前检查编码是否已存在</li>
     *   <li>更新模块时检查新编码是否被其他模块占用</li>
     * </ul>
     * </p>
     *
     * @param code 模块编码
     * @return 模块对象，如果不存在则返回 null
     */
    Module findByCode(@Param("code") String code);

    /**
     * 插入模块
     * <p>
     * 新增模块记录，自动生成主键 ID。
     * </p>
     *
     * @param module 模块对象
     * @return 影响的行数（始终为 1）
     */
    int insert(Module module);

    /**
     * 更新模块
     * <p>
     * 根据 ID 更新模块信息，支持部分字段更新（动态 SQL）。
     * </p>
     *
     * @param module 模块对象（必须包含 id 字段）
     * @return 影响的行数
     */
    int update(Module module);

    /**
     * 删除模块
     * <p>
     * 物理删除模块记录，不可恢复。
     * </p>
     *
     * @param id 模块 ID
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id);
}
