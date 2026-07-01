package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层接口，操作 user 表。
 */
@Mapper
public interface UserMapper {

    /** 根据用户名查询用户（登录时使用） */
    User findByUsername(@Param("username") String username);

    /** 根据 ID 查询用户（JWT 认证时验证用户状态） */
    User findById(@Param("id") Integer id);

    /** 插入新用户（注册时使用） */
    int insert(User user);

    /** 查询所有用户列表（管理员后台使用） */
    List<User> findAll();

    /** 更新用户的启用/禁用状态（管理员操作） */
    int updateEnabled(@Param("id") Integer id, @Param("enabled") Boolean enabled);

    /** 根据 ID 删除用户（管理员操作） */
    int deleteById(@Param("id") Integer id);

    /** 级联删除用户及其所有关联数据（调用存储过程） */
    void batchDeleteUser(@Param("id") Integer id);
}
