package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层接口（MyBatis Mapper）—— 操作 user 表
 *
 * 对应 XML 映射文件：src/main/resources/mapper/UserMapper.xml
 *
 * user 表结构：
 * - id：        主键，自增（Integer 类型）
 * - username：  用户名（唯一，登录账号）
 * - password：  密码（BCrypt 加密后的密文）
 * - nickname：  昵称（显示名称，可个性化设置）
 * - email：     电子邮箱
 * - avatar：    头像 URL
 * - role：      角色（"user" 或 "admin"）
 * - enabled：   是否启用（Boolean，管理员可禁用用户）
 * - create_time：注册时间
 *
 * ─── 密码安全 ───
 * password 字段存储的是 BCrypt 哈希值，不是明文密码。
 * BCrypt 的特征：以 $2a$10$ 开头，长度 60 字符。
 * 即使两个用户密码相同，存储在数据库中的哈希值也不同（自动加盐）。
 *
 * ─── 角色系统 ───
 * role 字段存储：user 或 admin（小写）
 * Spring Security 中权限检查时自动添加 ROLE_ 前缀变为 ROLE_USER / ROLE_ADMIN
 *
 * ─── 启用/禁用 ───
 * enabled 字段用于管理员控制用户登录权限：
 * - true：  用户可以正常登录和使用系统
 * - false： 用户被禁用，JWT 过滤器会拒绝该用户的认证（即使 Token 有效也不行）
 * 这是一个"软禁用"机制，无需删除用户数据。
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户（登录时使用）
     *
     * 用于登录认证：AuthController 接收用户名和密码，通过此方法查出用户，
     * 然后用 BCrypt 匹配密码，匹配成功则生成 JWT Token。
     *
     * @param username 用户名（唯一，不区分大小写？由数据库校对规则决定）
     * @return 用户对象，若用户名不存在返回 null
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据 ID 查询用户
     *
     * 在 JWT 认证过滤器中使用：从 Token 中解析出 userId，然后查库验证用户是否仍为启用状态。
     * 如果用户被管理员禁用（enabled = false），即使 Token 未过期也不能通过认证。
     *
     * @param id 用户 ID
     * @return 用户对象，若不存在返回 null
     */
    User findById(@Param("id") Integer id);

    /**
     * 插入新用户（注册时使用）
     *
     * 注意：
     * - 注册时传入的 password 已经是 BCrypt 加密后的密文（由 AuthService 负责加密）
     * - 默认 role 为 "user"（普通用户）
     * - 默认 enabled 为 true（注册后即启用）
     * - 插入成功后，自增主键回填到 user.id 属性
     *
     * @param user 用户对象（必须包含 username、password，推荐包含 nickname、email）
     * @return 受影响的行数（正常为 1）
     */
    int insert(User user);

    /**
     * 查询所有用户列表（管理员后台使用）
     *
     * 返回所有注册用户的信息，用于管理员管理用户。
     * 注意：出于安全考虑，返回的密码应该是已经脱敏的（但不脱敏也没关系，因为是 BCrypt 密文，无法反解）。
     * 更好的做法是查询时 SELECT 排除 password 字段。
     *
     * @return 所有用户的列表
     */
    List<User> findAll();

    /**
     * 更新用户的启用/禁用状态（管理员操作）
     *
     * 管理员可以通过此方法启用或禁用用户账号。
     * 被禁用的用户即使持有有效的 JWT Token 也无法通过认证过滤器（JwtAuthenticationFilter 会检查）。
     *
     * 典型使用场景：
     * - 用户违规 → 管理员禁用该用户
     * - 用户申诉 → 管理员重新启用该用户
     *
     * @param id      目标用户 ID
     * @param enabled true 为启用，false 为禁用
     * @return 受影响的行数（正常为 1）
     */
    int updateEnabled(@Param("id") Integer id, @Param("enabled") Boolean enabled);
}
