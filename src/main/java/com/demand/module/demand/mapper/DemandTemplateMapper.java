package com.demand.module.demand.mapper;

import com.demand.module.demand.entity.DemandTemplate;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 需求模板数据访问层
 * <p>
 * 提供需求模板的增删改查操作，支持按状态筛选和排序查询。
 * </p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li><b>查询启用模板</b>：返回 status=1 的模板，按 sort 升序排列</li>
 *   <li><b>查询所有模板</b>：返回全部模板（含禁用），用于后台管理</li>
 *   <li><b>CRUD 操作</b>：插入、更新、删除模板记录</li>
 * </ul>
 */
@Mapper
public interface DemandTemplateMapper {

    /**
     * 查询所有启用的模板
     * <p>
     * 返回 status=1 的模板列表，按排序号升序、创建时间降序排列。
     * 主要用于前端创建需求时的模板选择下拉框。
     * </p>
     * 
     * <p>
     * <b>SQL 示例</b>：
     * <pre>
     * SELECT * FROM demand_template 
     * WHERE status = 1 
     * ORDER BY sort ASC, create_time DESC
     * </pre>
     * </p>
     *
     * @return 启用的模板列表
     */
    @Select("SELECT * FROM demand_template WHERE status = 1 ORDER BY sort ASC, create_time DESC")
    List<DemandTemplate> selectAllEnabled();

    /**
     * 查询所有模板（含禁用）
     * <p>
     * 返回系统中的所有模板，不进行状态过滤。
     * 主要用于后台管理界面展示完整的模板列表。
     * </p>
     * 
     * <p>
     * <b>SQL 示例</b>：
     * <pre>
     * SELECT * FROM demand_template 
     * ORDER BY sort ASC, create_time DESC
     * </pre>
     * </p>
     *
     * @return 所有模板列表
     */
    @Select("SELECT * FROM demand_template ORDER BY sort ASC, create_time DESC")
    List<DemandTemplate> selectAll();

    /**
     * 根据 ID 查询模板
     *
     * @param id 模板 ID
     * @return 模板对象，不存在时返回 null
     */
    @Select("SELECT * FROM demand_template WHERE id = #{id}")
    DemandTemplate selectById(Long id);

    /**
     * 插入新模板
     * <p>
     * 新增一个需求模板记录，自动生成主键 ID。
     * </p>
     *
     * @param template 模板对象
     * @return 影响的行数（成功为 1）
     */
    @Insert("INSERT INTO demand_template(name, category, content, sort, status, create_time, update_time) " +
            "VALUES(#{name}, #{category}, #{content}, #{sort}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DemandTemplate template);

    /**
     * 更新模板
     * <p>
     * 修改模板的名称、分类、内容、排序号或状态。
     * </p>
     *
     * @param template 更新后的模板对象（必须包含 id）
     * @return 影响的行数（成功为 1）
     */
    @Update("UPDATE demand_template SET name=#{name}, category=#{category}, content=#{content}, " +
            "sort=#{sort}, status=#{status}, update_time=#{updateTime} WHERE id=#{id}")
    int update(DemandTemplate template);

    /**
     * 删除模板
     * <p>
     * 物理删除模板记录，不可恢复。
     * </p>
     *
     * @param id 模板 ID
     * @return 影响的行数（成功为 1）
     */
    @Delete("DELETE FROM demand_template WHERE id = #{id}")
    int deleteById(Long id);
}