package com.demand.module.attachment.mapper;

import com.demand.module.attachment.entity.Attachment;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 附件数据访问层（Mapper）
 * <p>
 * 提供附件数据的增删查改功能。
 * 所有 SQL 语句使用注解方式定义，无需 XML 配置文件。
 * </p>
 * 
 * <h3>核心方法：</h3>
 * <ul>
 *   <li>{@link #findByDemandId} - 查询需求的所有附件</li>
 *   <li>{@link #findByFileHash} - 根据 MD5 哈希值查找文件（用于去重）</li>
 *   <li>{@link #insert} - 插入附件记录</li>
 *   <li>{@link #deleteById} - 删除附件记录</li>
 * </ul>
 */
@Mapper
public interface AttachmentMapper {

    /**
     * 查询需求的所有附件
     * <p>
     * 返回指定需求的所有附件，按创建时间倒序排列（最新的在前）。
     * </p>
     *
     * @param demandId 需求 ID
     * @return 附件列表
     */
    @Select("SELECT * FROM attachment WHERE demand_id = #{demandId}")
    List<Attachment> findByDemandId(Long demandId);

    /**
     * 根据 ID 查询附件
     * <p>
     * 用于下载、删除操作前的存在性检查。
     * </p>
     *
     * @param id 附件 ID
     * @return 附件对象，如果不存在则返回 null
     */
    @Select("SELECT * FROM attachment WHERE id = #{id}")
    Attachment findById(Long id);

    /**
     * 插入附件记录
     * <p>
     * 新增附件信息，自动生成主键 ID。
     * </p>
     *
     * @param attachment 附件对象
     * @return 影响的行数（始终为 1）
     */
    @Insert("INSERT INTO attachment(demand_id, file_name, file_path, file_type, file_size, uploader_id, create_time) " +
            "VALUES(#{demandId}, #{fileName}, #{filePath}, #{fileType}, #{fileSize}, #{uploaderId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Attachment attachment);

    /**
     * 删除附件记录
     * <p>
     * 物理删除附件的数据库记录。
     * 注意：此方法只删除数据库记录，物理文件由 Service 层负责删除。
     * </p>
     *
     * @param id 附件 ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM attachment WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据文件哈希值查询附件
     * <p>
     * 用于文件去重：如果数据库中已存在相同 MD5 值的文件，则复用该记录，避免重复存储。
     * </p>
     * 
     * <p>
     * <b>使用场景</b>：
     * <ul>
     *   <li>用户上传文件前计算 MD5 哈希值</li>
     *   <li>查询数据库中是否已存在相同哈希值的文件</li>
     *   <li>如果存在，直接复用记录；如果不存在，才上传新文件</li>
     * </ul>
     * </p>
     *
     * @param fileHash 文件 MD5 哈希值
     * @return 附件对象，如果不存在则返回 null
     */
    @Select("SELECT * FROM attachment WHERE file_hash = #{fileHash}")
    Attachment findByFileHash(String fileHash);
}
