package org.example.musicdemo.entity;
import java.time.LocalDateTime;
import lombok.Data;
/**
 * 用户实体：对应 user 表
 * 字段名使用驼峰命名，与数据库的下划线命名对应
 * （因为 application.yml 中配置了 map-underscore-to-camel-case: true）
 */
@Data
public class User {
    /** 主键 ID */
    private Integer id;
    /** 登录用户名 */
    private String username;
    /** 登录密码 */
    private String password;
    /** 显示昵称 */
    private String name;
    /** 邮箱 */
    private String email;
    /** 角色：ROLE_USER 或 ROLE_ADMIN */
    private String role;
    /** 是否启用 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createTime;
}