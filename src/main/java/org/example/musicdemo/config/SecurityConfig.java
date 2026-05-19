package org.example.musicdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 *
 * @Configuration      表示这是一个配置类
 * @EnableWebSecurity  启用 Spring Security 的 Web 安全功能
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级别的权限控制（@PreAuthorize 等）
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器 Bean
     * BCryptPasswordEncoder 是一种安全的单向哈希算法
     * 相同的明文每次加密结果都不同（自带随机盐）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链（核心配置）
     *
     * 请求处理流程：
     *   请求 → JwtAuthenticationFilter → HttpSecurity 规则匹配 → Controller
     *
     * URL 权限规则设计思路：
     *   游客（未登录）可以：浏览音乐列表、看详情、看排行榜、登录、注册、下载文件
     *   登录用户（user/admin）可以：上传音乐、评论、点赞、下载
     *   管理员（admin）可以：用户管理、删除音乐、删除评论
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（因为我们是 RESTful API，用 Token 认证，不需要 CSRF 保护）
                .csrf(csrf -> csrf.disable())

                // 无状态会话（不使用 HttpSession，完全依赖 JWT）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL 权限配置
                .authorizeHttpRequests(auth -> auth
                        // ===== 公开接口（游客可访问） =====
                        .requestMatchers("/api/auth/**").permitAll()           // 登录、注册
                        .requestMatchers("/api/music/file/**").permitAll()     // 音乐文件播放
                        .requestMatchers("/api/music/list").permitAll()               // 列表
                        .requestMatchers(HttpMethod.GET, "/api/music/*").permitAll()  // 详情
                        .requestMatchers(HttpMethod.GET, "/api/music/*/stream").permitAll()  // 播放
                        .requestMatchers(HttpMethod.GET, "/api/ranking/**").permitAll() // 排行榜

                        // ===== 管理员专用接口 =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== 其他所有 /api/** 接口需要登录 =====
                        .requestMatchers("/api/**").authenticated()

                        // ===== 静态资源（前端页面）允许所有 =====
                        .anyRequest().permitAll()
                )

                // 把 JWT 过滤器加到 UsernamePasswordAuthenticationFilter 之前
                // 这样 JWT 过滤器会先执行，解析出用户信息后设置认证信息
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}