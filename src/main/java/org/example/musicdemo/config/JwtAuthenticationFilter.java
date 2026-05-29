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
 * JWT 认证过滤器 —— 整个后端安全体系的"守门员"
 *
 * 每个 HTTP 请求都会经过这个过滤器（继承 OncePerRequestFilter 确保每个请求只过滤一次）。
 *
 * ─── 核心工作流程 ───
 * 1. 从 HTTP 请求头 "Authorization" 中提取 Token 值（格式为 "Bearer xxx.xxx.xxx"）
 * 2. 检查 Token 是否存在且以 "Bearer " 开头
 * 3. 取出后面的 JWT 字符串，调用 JwtUtils 验证其签名和有效期
 * 4. 验证通过后，解析 Token 中存储的用户 ID、用户名、角色
 * 5. 从数据库查询该用户是否仍处于"启用"状态
 * 6. 如果一切正常，将用户信息封装为 Spring Security 的 Authentication 对象
 * 7. 将 Authentication 对象设置到 SecurityContextHolder 中
 * 8. 后续 Controller 或 Service 可以通过 SecurityContextHolder 获取当前用户身份
 *
 * ─── 关键设计 ───
 * - 不拦截任何请求 —— 即使 Token 无效也放行（让 SecurityConfig 的 URL 权限规则去拦截）
 * - 如果 Token 有效且用户启用，就设置认证信息；否则不设置（用户未登录/匿名）
 * - 必须调用 filterChain.doFilter() 放行请求，否则页面永远空白
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类：负责 Token 的生成、解析、验证 */
    private final JwtUtils jwtUtils;

    /** 用户 Mapper：用于从数据库查询用户状态，确保被禁用的用户无法通过认证 */
    private final UserMapper userMapper;

    /**
     * 构造器注入（Spring 官方推荐的注入方式）
     *
     * @param jwtUtils   JWT 工具类，由 Spring 自动注入
     * @param userMapper 用户数据访问对象，由 Spring 自动注入
     */
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