package org.example.musicdemo.controller;


import io.jsonwebtoken.Claims;
import org.example.musicdemo.common.Result;
import org.example.musicdemo.config.JwtUtils;
import org.example.musicdemo.entity.User;
import org.example.musicdemo.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器，处理用户注册、登录和 Token 验证。
 * 所有端点以 /api/auth 开头，SecurityConfig 中配置为 permitAll（无需认证）。
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

    /** 用户登录，成功返回 JWT Token */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }

        if (user.getEnabled() != null && !user.getEnabled()) {
            return Result.fail("账号已被禁用");
        }

        if (!userService.validatePassword(password, user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return Result.success(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    /** 验证 Token 有效性（前端页面刷新时调用） */
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

    /** 用户注册 */
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
