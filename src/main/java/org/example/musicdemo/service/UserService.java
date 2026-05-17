package org.example.musicdemo.service;


import org.example.musicdemo.entity.User;
import org.example.musicdemo.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务
 * @Service 表示这是 Spring 管理的业务层 Bean
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造器注入（推荐方式，比 @Autowired 字段注入更好）
     * Spring 会自动把 UserMapper 和 PasswordEncoder 传入
     */
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 根据用户名查用户 */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /** 注册：密码需要 BCrypt 加密后才能存入数据库 */
    @Transactional
    public User register(String username, String password, String name, String email) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        // BCrypt 加密：相同的明文每次生成的密文都不同
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name != null && !name.isBlank() ? name : username);  // 如果没填昵称就用用户名
        user.setEmail(email != null && !email.isBlank() ? email : null);
        user.setEnabled(true);
        user.setRole("user");  // 注册的用户默认是普通用户

        userMapper.insert(user);
        return user;
    }

    /** 验证密码 */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /** 查询所有用户（管理员用） */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    /** 切换用户启用/禁用状态 */
    @Transactional
    public void toggleEnabled(Integer id, Boolean enabled) {
        userMapper.updateEnabled(id, enabled);
    }
}