package org.example.musicdemo.config;


import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.musicdemo.entity.User;
import org.example.musicdemo.mapper.UserMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 每个请求都会经过这个过滤器（继承 OncePerRequestFilter 确保每个请求只过滤一次）
 *
 * 工作流程：
 * 1. 从 HTTP Header 中取出 Authorization 的值
 * 2. 检查是否是 "Bearer " 开头
 * 3. 如果是，取出后面的 Token 部分
 * 4. 验证 Token，提取 user_id、username、role
 * 5. 设置到 Spring Security 的 SecurityContextHolder 中
 * 6. 后续的 Controller 可以通过 SecurityContextHolder 获取当前用户信息
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserMapper userMapper) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头中提取 Token
        String token = extractToken(request);

        // 2. 如果 Token 存在且有效
        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            // 3. 解析 Token
            Claims claims = jwtUtils.parseToken(token);
            Integer userId = Integer.parseInt(claims.getSubject());
            String username = (String) claims.get("username");
            String role = (String) claims.get("role");

            // 4. 校验用户是否仍处于启用状态
            User user = userMapper.findById(userId);
            if (user != null && Boolean.TRUE.equals(user.getEnabled())) {
                // 5. 构建认证对象
                // Spring Security 中，权限需要 ROLE_ 前缀
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. 设置到安全上下文中（后续代码可以通过 SecurityContextHolder 拿到）
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 6. 继续执行后面的过滤器（很重要！不能忘记）
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     * Authorization 头的格式：Bearer eyJhbGciOiJIUzI1NiJ9...
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 去掉 "Bearer " 前缀（7个字符）
        }
        return null;
    }
}