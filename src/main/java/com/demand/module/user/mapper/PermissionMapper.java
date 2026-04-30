package com.demand.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID查询系统访问权限信息
     */
    List<String> selectPermissionKeysByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询完整的权限列表（包含所有字段）
     */
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询系统访问权限信息
     */
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}
