package org.example.musicdemo.service;


import org.example.musicdemo.entity.User;
import org.example.musicdemo.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务 —— 负责用户注册、登录验证、用户管理等功能。
 * <p>
 * 核心业务逻辑：
 * <ul>
 *   <li><b>注册</b>：密码使用 BCrypt 加密后入库，检测用户名唯一性</li>
 *   <li><b>登录验证</b>：使用 {@link PasswordEncoder#matches} 比对明文密码和密文</li>
 *   <li><b>管理员功能</b>：查询所有用户、启用/禁用用户</li>
 * </ul>
 * </p>
 *
 * <h3>安全设计</h3>
 * <ul>
 *   <li>密码永远不存明文，统一使用 Spring Security 的 BCrypt 加密</li>
 *   <li>新注册用户默认角色为 {@code ROLE_USER}，仅能访问普通接口</li>
 *   <li>用户名唯一性在服务层校验（数据库层也有唯一索引兜底）</li>
 * </ul>
 */
@Service
public class UserService {

    /** 用户表的数据访问层接口 */
    private final UserMapper userMapper;

    /**
     * Spring Security 提供的密码编码器。
     * 默认实现为 BCryptPasswordEncoder，使用 BCrypt 强哈希算法。
     * BCrypt 自动加盐，相同明文每次加密结果不同。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造器注入（符合 AGENTS.md 约定的注入方式，避免 @Autowired 字段注入）。
     * Spring 会自动从容器中注入 UserMapper 和 PasswordEncoder 实例。
     *
     * @param userMapper       用户 Mapper
     * @param passwordEncoder  密码编码器（BCrypt）
     */
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据用户名查询用户信息。
     * 用于登录流程中的身份校验。
     *
     * @param username 用户名
     * @return User 实体，如果用户不存在则返回 null
     */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 用户注册。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>检查用户名是否已被占用（唯一性校验）</li>
     *   <li>使用 BCrypt 对明文密码进行加密</li>
     *   <li>昵称默认取用户名（如果未填写）</li>
     *   <li>邮箱可选，空字符串转为 null</li>
     *   <li>默认启用账户、角色为普通用户</li>
     *   <li>插入数据库</li>
     * </ol>
     * </p>
     *
     * <h3>事务说明</h3>
     * 标注了 {@link Transactional}，但注册操作只有一条 INSERT，
     * 事务在此更多是为了预留扩展性（例如后续可能同时插入用户设置表）。
     *
     * @param username 用户名（必填，全局唯一）
     * @param password 明文密码（将由 BCrypt 加密存储）
     * @param name     用户昵称（可选，为空则使用 username）
     * @param email    电子邮箱（可选）
     * @return 已入库的完整 User 对象（包含自增 ID）
     * @throws RuntimeException 如果用户名已存在
     */
    @Transactional
    public User register(String username, String password, String name, String email) {
        // 检查用户名唯一性（数据库有唯一索引，但提前检查可以返回友好提示）
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        // BCrypt 加密：即使两个用户密码相同，加密后的密文也不同
        user.setPassword(passwordEncoder.encode(password));
        // 昵称为空时使用用户名作为默认昵称
        user.setName(name != null && !name.isBlank() ? name : username);
        // 邮箱为空字符串时存为 null（保持数据库干净）
        user.setEmail(email != null && !email.isBlank() ? email : null);
        user.setEnabled(true);           // 默认启用
        user.setRole("user");            // 默认为普通用户角色

        userMapper.insert(user);
        return user;
    }

    /**
     * 验证明文密码是否与数据库存储的密文匹配。
     * <p>
     * 通过 {@link PasswordEncoder#matches(String, String)} 完成。
     * BCrypt 的密文中包含了盐值，因此无需额外存储盐值字段。
     * </p>
     *
     * @param rawPassword     用户输入的明文密码
     * @param encodedPassword 数据库中存储的密文
     * @return true 如果密码匹配，false 否则
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 查询所有用户列表（仅管理员可用）。
     * 返回数据中不包含密码字段（由 MyBatis 映射控制或手动置空）。
     *
     * @return 所有用户的列表
     */
    public List<User> findAll() {
        return userMapper.findAll();
    }

    /**
     * 切换用户的启用/禁用状态。
     * <p>
     * 被禁用的用户无法登录系统。
     * 此操作用于管理员后台管理。
     * </p>
     *
     * @param id      用户 ID
     * @param enabled true 表示启用，false 表示禁用
     */
    @Transactional
    public void toggleEnabled(Integer id, Boolean enabled) {
        userMapper.updateEnabled(id, enabled);
    }
}