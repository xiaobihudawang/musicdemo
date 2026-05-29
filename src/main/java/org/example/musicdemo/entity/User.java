package org.example.musicdemo.entity;
import java.time.LocalDateTime;
import lombok.Data;
/**
 * 用户实体，对应数据库中的 user 表。
 *
 * <p>这是系统的用户模型，用于认证、授权和用户信息管理。
 * 所有字段采用驼峰命名法，利用 MyBatis 的 map-underscore-to-camel-case
 * 配置自动映射数据库的下划线命名（如 create_time → createTime）。</p>
 *
 * <p>数据库表结构（user）：</p>
 * <pre>
 * id          INT PRIMARY KEY AUTO_INCREMENT
 * username    VARCHAR(50) NOT NULL UNIQUE   -- 登录用户名
 * password    VARCHAR(255) NOT NULL         -- 加密后的密码（BCrypt）
 * name        VARCHAR(50)                   -- 显示昵称
 * email       VARCHAR(100)                  -- 邮箱
 * role        VARCHAR(20) DEFAULT 'ROLE_USER' -- 角色
 * enabled     TINYINT(1) DEFAULT 1          -- 是否启用
 * create_time DATETIME DEFAULT CURRENT_TIMESTAMP
 * </pre>
 *
 * <p>安全相关：</p>
 * <ul>
 *   <li><b>密码存储</b>：password 字段存储的是 BCrypt 加密后的密文，
 *       绝不保存明文密码。注册时使用 {@code BCryptPasswordEncoder.encode()} 加密，
 *       登录时使用 {@code BCryptPasswordEncoder.matches()} 校验。</li>
 *   <li><b>角色体系</b>：role 字段存储 Spring Security 标准的角色名，
 *       普通用户为 {@code ROLE_USER}，管理员为 {@code ROLE_ADMIN}。
 *       在 Security 配置中，{@code /api/admin/**} 路径需要 {@code ROLE_ADMIN} 权限。</li>
 *   <li><b>启用状态</b>：enabled 字段用于控制用户是否允许登录。
 *       如果账号被禁用（enabled = false），Spring Security 的 {@code UserDetails.isEnabled()}
 *       返回 false，登录时会被拒绝。当前版本预留此字段，管理后台可操作。</li>
 * </ul>
 *
 * <p>该实体用于多个场景：</p>
 * <ul>
 *   <li>AuthController：注册（INSERT）、登录（查询并校验密码）</li>
 *   <li>UserService：封装了按用户名查询、按 ID 查询等方法</li>
 *   <li>其他实体（Comment、Music 等）经常需要关联查询用户昵称，通过 username 字段进行展示</li>
 * </ul>
 *
 * @see org.example.musicdemo.config.SecurityConfig
 * @see org.example.musicdemo.service.UserService
 * @see org.example.musicdemo.controller.AuthController
 */
@Data
public class User {
    /** 主键 ID，自增长，唯一标识每个用户 */
    private Integer id;

    /**
     * 登录用户名，注册时用户自定义。
     * 数据库表中设置了 UNIQUE 约束，确保用户名全局唯一，
     * 注册时 UserService 会检查是否已存在同名用户。
     */
    private String username;

    /**
     * 登录密码，存储的是 BCrypt 加密后的密文。
     *
     * <p>重要安全说明：</p>
     * <ul>
     *   <li>永远不会以明文形式存储在数据库中。</li>
     *   <li>在 API 响应中，密码字段不会被序列化返回（通常通过 @JsonIgnore 或在数据脱敏层处理）。</li>
     *   <li>注册时由 {@code BCryptPasswordEncoder} 加密后再写入数据库。</li>
     *   <li>校验时调用 {@code encoder.matches(plainPassword, encodedPassword)} 比较。</li>
     * </ul>
     */
    private String password;

    /**
     * 用户显示昵称，展示在音乐列表、评论等页面。
     * 区别于 username（用于登录），此字段可以随时修改，
     * 允许重复，建议前端限制长度（如 1~20 个字符）。
     */
    private String name;

    /**
     * 用户邮箱地址，预留字段，当前版本暂未用于认证或通知功能。
     * 后续可用于邮箱验证、密码找回、消息推送等场景。
     * 数据库表中未设置 UNIQUE 约束，允许多个用户使用同一邮箱。
     */
    private String email;

    /**
     * 用户角色标识，Spring Security 标准角色格式。
     *
     * <p>可选值：</p>
     * <ul>
     *   <li><b>ROLE_USER</b>：普通用户，可以浏览音乐、播放/下载、评论、点赞。
     *       这是注册时的默认角色。</li>
     *   <li><b>ROLE_ADMIN</b>：管理员，拥有所有权限，包括上传管理、用户管理、
     *       系统设置。由超级管理员在数据库中手动设置，注册接口不会赋予此角色。</li>
     * </ul>
     *
     * <p>在 Spring Security 的 {@code hasRole()} 判断时，会自动去除 "ROLE_" 前缀。
     * 例如 {@code hasRole("ADMIN")} 等效于检查 {@code role.equals("ROLE_ADMIN")}。</p>
     */
    private String role;

    /**
     * 用户账号是否已启用（可用）。
     *
     * <p>对应 Spring Security 的 {@code UserDetails.isEnabled()} 方法。
     * 当 enabled = false 时，认证提供者会抛出 {@code DisabledException}，登录失败。</p>
     *
     * <p>此字段为数据库中的 TINYINT(1)，在 Java 中使用 Boolean 类型，
     * MyBatis 自动处理 0/1 → false/true 的转换。</p>
     */
    private Boolean enabled;

    /**
     * 用户注册时间，由数据库自动填充 DEFAULT CURRENT_TIMESTAMP。
     * 可用于展示用户注册天数、统计每日新增用户数等运营分析场景。
     */
    private LocalDateTime createTime;
}