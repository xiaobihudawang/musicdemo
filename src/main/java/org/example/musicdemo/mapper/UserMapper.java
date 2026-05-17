package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户映射接口：对应 user 表
 */
@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Integer id);
    int insert(User user);
    List<User> findAll();
    int updateEnabled(@Param("id") Integer id, @Param("enabled") Boolean enabled);
}
