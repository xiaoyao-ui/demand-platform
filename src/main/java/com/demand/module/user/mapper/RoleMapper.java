package com.demand.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demand.module.user.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    
    /**
     * 根据用户ID查询其拥有的所有角色标识
     */
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);
    
    /**
     * 根据角色标识查询角色ID列表
     */
    List<Long> selectRoleIdsByKeys(@Param("roleKeys") List<String> roleKeys);
    
    /**
     * 根据角色code查询角色ID列表
     */
    List<Long> selectRoleIdsByCodes(@Param("roleCodes") List<Integer> roleCodes);
    
    /**
     * 根据角色ID查询拥有该角色的所有用户ID
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
