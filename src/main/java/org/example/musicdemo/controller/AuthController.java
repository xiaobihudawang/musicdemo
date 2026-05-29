package org.example.musicdemo.controller;


import io.jsonwebtoken.Claims;
import org.example.musicdemo.common.Result;
import org.example.musicdemo.config.JwtUtils;
import org.example.musicdemo.entity.User;
import org.example.musicdemo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 —— 处理用户注册、登录和 Token 验证。
 * <p>
 * 安全说明：
 * - 此控制器下的所有端点（/api/auth/**）在 SecurityConfig 中配置为 permitAll()（无需认证）。
 * - 原因：用户尚未登录时也需要能够访问注册和登录接口。
 * - 登录成功后发放 JWT（JSON Web Token），后续请求在 Authorization 头中携带。
 * - 前端将 token 保存在 localStorage 的 music_token 键中。
 * <p>
 * 涉及组件：
 * - UserService：用户查询、密码验证、注册逻辑。
 * - JwtUtils：JWT 的生成、解析和验证工具类（基于 jjwt 0.12.6）。
 * <p>
 * 密码存储说明：
 * - 注册时对明文密码进行加密（BCrypt 或自定义哈希）。
 * - 登录验证时使用 userService.validatePassword() 比对明文与密文。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 用户服务 —— 处理用户持久化和密码校验 */
    private final UserService userService;
    /** JWT 工具类 —— 负责生成、解析和验证 JSON Web Token */
    private final JwtUtils jwtUtils;

    /**
     * 构造器注入 —— 无 @Autowired，利用 Spring 隐式自动注入。
     *
     * @param userService 用户服务
     * @param jwtUtils    JWT 工具类
     */
    public AuthController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 用户登录
     * <p>
     * URL: POST /api/auth/login
     * <p>
     * 请求体 JSON 格式：{"username": "用户名", "password": "密码"}
     * <p>
     * 权限：公开（无需 Token）。
     * <p>
     * 执行流程：
     * 1. 从请求体 Map 中提取 username 和 password。
     * 2. 调用 userService.findByUsername(username) 查询用户。
     *    - 若用户不存在，返回"用户名或密码错误"（不明确提示是用户名还是密码错，防枚举攻击）。
     * 3. 检查用户 enabled 字段：
     *    - 若 enabled == false（被管理员禁用），返回"账号已被禁用"。
     * 4. 调用 userService.validatePassword(password, user.getPassword()) 验证密码。
     *    - 验证失败返回与第 2 步相同的模糊消息。
     * 5. 验证通过后调用 jwtUtils.generateToken(id, username, role) 生成 JWT。
     *    - Token 中封装了 userId（sub）、username、role 等声明。
     *    - Token 有过期时间（由 application.yml 中的 expiration 配置，默认可能 20 秒或更长）。
     * 6. 返回 token、username、role 给前端。
     * <p>
     * 返回值：
     * - 成功：{"code":200, "data":{"token":"xxx", "username":"admin", "role":"admin"}}
     * - 失败：{"code":400, "message":"用户名或密码错误"}
     * - 禁用：{"code":400, "message":"账号已被禁用"}
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        // 1. 根据用户名查找用户 —— 不存在说明用户名错误
        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 2. 检查账号是否被管理员禁用（enabled = false）
        if (user.getEnabled() != null && !user.getEnabled()) {
            return Result.fail("账号已被禁用");
        }

        // 3. 验证明文密码与数据库中加密存储的密码是否匹配
        if (!userService.validatePassword(password, user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        // 4. 密码验证通过，生成 JWT Token
        //    Token 中包含 userId、username、role 三个核心声明
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 返回 Token 和用户基本信息给前端
        //    前端将 Token 存储在 localStorage 的 music_token 键中
        return Result.success(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    /**
     * 验证 Token 有效性（前端自动登录 / 页面刷新时调用）
     * <p>
     * URL: GET /api/auth/verify
     * <p>
     * 请求头：Authorization: Bearer <token>
     * <p>
     * 权限：公开（因为此端点本身就是为了验证 Token 而存在，调用时可能尚未持有有效 Token）。
     * 但实际上前端会在请求头中携带 Token，后端解析验证。
     * <p>
     * 业务说明：
     * - 用于前端检查 localStorage 中存储的 Token 是否仍然有效。
     * - 例如：用户刷新页面时，前端调用此接口验证 Token 未过期，
     *   若有效则直接进入首页，否则跳转到登录页。
     * - 验证分为两步：
     *   1. 检查 Authorization 头是否存在且以 "Bearer " 开头。
     *   2. 调用 jwtUtils.validateToken(token) 验证签名和过期时间。
     * - 验证通过后解析 Token 中的 Claims，返回 userId、username、role。
     * <p>
     * 返回值：
     * - Token 有效：{"code":200, "data":{"userId":1, "username":"admin", "role":"admin"}}
     * - 无 Token：{"code":400, "message":"未提供有效的认证信息"}
     * - Token 无效/过期：{"code":400, "message":"Token 已过期或无效"}
     */
    @GetMapping("/verify")
    public Result<?> verify(@RequestHeader("Authorization") String authHeader) {
        // 检查请求头是否存在且格式正确（必须为 "Bearer xxx"）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail("未提供有效的认证信息");
        }
        // 截取 "Bearer " 之后的实际 Token 字符串（长度为 7）
        String token = authHeader.substring(7);
        // 验证 Token 的签名是否合法以及是否在有效期内
        if (!jwtUtils.validateToken(token)) {
            return Result.fail("Token 已过期或无效");
        }
        // 解析 Token 载荷，提取用户信息返回给前端
        Claims claims = jwtUtils.parseToken(token);
        return Result.success(Map.of(
                "userId", Integer.parseInt(claims.getSubject()),
                "username", claims.get("username"),
                "role", claims.get("role")
        ));
    }

    /**
     * 用户注册
     * <p>
     * URL: POST /api/auth/register
     * <p>
     * 请求体 JSON：{"username": "用户名", "password": "密码", "name": "昵称", "email": "邮箱"}
     * <p>
     * 权限：公开（无需 Token）。
     * <p>
     * 业务说明：
     * - 调用 userService.register(username, password, name, email) 完成注册。
     * - Service 层会执行以下操作：
     *   1. 校验 username 是否已存在（唯一约束），若已存在抛出 RuntimeException。
     *   2. 对 password 进行加密（BCryptPasswordEncoder 等）。
     *   3. 设置默认 role = "user"（普通用户角色）。
     *   4. 设置默认 enabled = true（账号默认启用）。
     *   5. 插入数据库。
     * - 注册成功后返回新用户的 id 和 username。
     * - 注册成功后不会自动登录（前端需跳转到登录页）。
     * <p>
     * 异常处理：
     * - userService.register() 抛出的 RuntimeException 被 catch 块捕获，
     *   错误消息直接返回给前端（如"用户名已被注册"）。
     * <p>
     * 返回值：
     * - 成功：{"code":200, "data":{"id":1, "username":"newuser"}}
     * - 失败：{"code":400, "message":"用户名已被注册"}
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> data) {
        try {
            User user = userService.register(
                    data.get("username"),   // 用户名（必填）
                    data.get("password"),   // 密码（必填）
                    data.get("name"),       // 昵称（可选）
                    data.get("email")       // 邮箱（可选）
            );
            return Result.success(Map.of("id", user.getId(), "username", user.getUsername()));
        } catch (RuntimeException e) {
            // 注册过程中的业务异常（如用户名重复）直接以消息形式返回
            return Result.fail(e.getMessage());
        }
    }
}
