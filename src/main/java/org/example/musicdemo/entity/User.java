package org.example.musicdemo.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户实体，对应数据库中的 user 表。
 * 用于认证、授权和用户信息管理。
 */
@Data
public class User {
    /** 主键 ID，自增长 */
    private Integer id;

    /** 登录用户名，全局唯一 */
    private String username;

    /** 登录密码，BCrypt 加密存储，不返回给前端（@JsonIgnore） */
    @JsonIgnore
    private String password;

    /** 显示昵称，可重复 */
    private String name;

    /** 邮箱地址，预留字段 */
    private String email;

    /** 角色：ROLE_USER 或 ROLE_ADMIN */
    private String role;

    /** 是否启用，禁用后无法登录 */
    private Boolean enabled;

    /** 注册时间，数据库自动填充 */
    private LocalDateTime createTime;
}