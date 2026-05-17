package org.example.musicdemo.controller;


import io.jsonwebtoken.Claims;
import org.example.musicdemo.common.Result;
import org.example.musicdemo.config.JwtUtils;
import org.example.musicdemo.entity.User;
import org.example.musicdemo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * @RestController = @Controller + @ResponseBody（所有方法返回 JSON）
 * @RequestMapping("/api/auth") 这个类的所有请求路径都以 /api/auth 开头
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    public AuthController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 登录
     * POST /api/auth/login
     * 请求体 JSON：{"username": "admin", "password": "123"}
     * @RequestBody 自动把 JSON 转成 Java Map
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        // 1. 查用户
        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        // 2. 检查是否被禁用
        if (user.getEnabled() != null && !user.getEnabled()) {
            return Result.fail("账号已被禁用");
        }

        // 3. 验证密码
        if (!userService.validatePassword(password, user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        // 4. 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 返回 token + 用户信息
        return Result.success(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    /**
     * 验证 Token 是否有效（前端自动登录时调用）
     * GET /api/auth/verify
     * 从 Authorization 头中提取 Token，验证签名和过期时间
     */
    @GetMapping("/verify")
    public Result<?> verify(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.fail("未提供有效的认证信息");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return Result.fail("Token 已过期或无效");
        }
        Claims claims = jwtUtils.parseToken(token);
        return Result.success(Map.of(
                "userId", Integer.parseInt(claims.getSubject()),
                "username", claims.get("username"),
                "role", claims.get("role")
        ));
    }

    /**
     * 注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> data) {
        try {
            User user = userService.register(
                    data.get("username"),
                    data.get("password"),
                    data.get("name"),
                    data.get("email")
            );
            return Result.success(Map.of("id", user.getId(), "username", user.getUsername()));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
