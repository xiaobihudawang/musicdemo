package org.example.musicdemo.service;


import org.example.musicdemo.entity.User;
import org.example.musicdemo.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务，负责用户注册、登录验证、用户管理等功能。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 根据用户名查询用户 */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /** 用户注册（密码 BCrypt 加密，检测用户名唯一性） */
    @Transactional
    public User register(String username, String password, String name, String email) {
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name != null && !name.isBlank() ? name : username);
        user.setEmail(email != null && !email.isBlank() ? email : null);
        user.setEnabled(true);
        user.setRole("user");

        userMapper.insert(user);
        return user;
    }

    /** 验证明文密码是否与密文匹配 */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /** 查询所有用户列表（管理员用） */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    /** 切换用户启用/禁用状态 */
    @Transactional
    public void toggleEnabled(Integer id, Boolean enabled) {
        userMapper.updateEnabled(id, enabled);
    }
}