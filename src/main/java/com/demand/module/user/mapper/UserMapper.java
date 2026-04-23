package com.demand.module.user.mapper;

import com.demand.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     */
    User findById(Long id);

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    User findByEmail(String email);

    /**
     * 根据手机号查询用户
     */
    User findByPhone(String phone);

    /**
     * 根据角色查询用户列表
     */
    List<User> findByRole(Integer role);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    List<User> selectAllUsers();

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int update(User user);

    /**
     * 更新用户角色
     */
    int updateRole(@Param("id") Long id, @Param("role") Integer role);

    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除用户
     */
    int deleteById(Long id);
}
