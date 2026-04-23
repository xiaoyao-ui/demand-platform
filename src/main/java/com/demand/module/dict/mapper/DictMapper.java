package com.demand.module.dict.mapper;

import com.demand.module.dict.entity.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 数据字典数据访问层（Mapper）
 * <p>
 * 提供字典数据的查询功能，支持按类型、编码筛选。
 * 所有 SQL 语句使用注解方式定义，无需 XML 配置文件。
 * </p>
 */
@Mapper
public interface DictMapper {

    /**
     * 根据类型查询字典列表
     * <p>
     * 只返回状态为启用（status=1）的字典项，并按排序号升序排列。
     * </p>
     * 
     * <p>
     * <b>SQL 示例</b>：
     * <pre>{@code
     * SELECT * FROM dict 
     * WHERE type = 'demand_type' AND status = 1 
     * ORDER BY sort ASC
     * }</pre>
     * </p>
     *
     * @param type 字典类型
     * @return 字典列表
     */
    @Select("SELECT * FROM dict WHERE type = #{type} AND status = 1 ORDER BY sort ASC")
    List<Dict> findByType(String type);

    /**
     * 根据类型和编码查询单个字典项
     * <p>
     * 用于将数字编码转换为中文名称，例如：
     * <ul>
     *   <li>输入：type="priority", code=2 → 输出：name="高"</li>
     * </ul>
     * </p>
     *
     * @param type 字典类型
     * @param code 字典编码
     * @return 字典对象，如果不存在则返回 null
     */
    @Select("SELECT * FROM dict WHERE type = #{type} AND code = #{code}")
    Dict findByTypeAndCode(String type, Integer code);

    /**
     * 查询所有字典类型
     * <p>
     * 返回系统中所有已启用的字典类型（去重），用于字典管理界面展示。
     * </p>
     *
     * @return 字典类型列表（如 ["demand_type", "priority", "demand_status"]）
     */
    @Select("SELECT DISTINCT type FROM dict WHERE status = 1")
    List<String> findAllTypes();
}
