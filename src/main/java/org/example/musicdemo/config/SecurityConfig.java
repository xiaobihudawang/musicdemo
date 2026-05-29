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
 * Spring Security 安全配置类 —— 整个后端的"安全大门"
 *
 * 本类负责三件事：
 * 1. 配置密码编码器（BCrypt 哈希算法，用于加密用户密码）
 * 2. 配置 URL 访问权限规则（哪些接口谁可以访问）
 * 3. 将自定义的 JWT 认证过滤器插入到 Spring Security 过滤器链中
 *
 * ─── 权限设计总览 ───
 * 游客（未登录）         → 浏览音乐列表/详情/排行榜、注册/登录、播放/下载音乐文件
 * 普通用户（ROLE_USER）  → 游客的全部权限 + 上传音乐、发表评论、点赞、记录下载
 * 管理员（ROLE_ADMIN）   → 以上全部 + 用户管理（启用/禁用）、删除音乐、删除评论
 *
 * ─── 过滤器链顺序 ───
 * 请求进入 → JwtAuthenticationFilter（自定义，最先执行）→ ... → UsernamePasswordAuthenticationFilter（Spring 内置）→ Controller
 *
 * ─── 关键技术点 ───
 * - 禁用 CSRF：因为使用 JWT 做无状态认证，不需要 CSRF 保护
 * - 无状态会话（STATELESS）：不用 HttpSession，每一个请求都独立验证 Token
 * - @EnableMethodSecurity：开启方法级注解 @PreAuthorize("hasRole('ADMIN')")，达到更精细的控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级别的权限控制（可以在任意 @Controller 或 @Service 方法上使用 @PreAuthorize 注解）
public class SecurityConfig {

    /**
     * 自定义的 JWT 认证过滤器 —— 由 Spring 自动注入
     * 这个过滤器会在每个请求进来时尝试从 Header 中解析 JWT Token，设置用户认证信息
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造器注入（Spring 官方推荐的方式）
     *
     * @param jwtAuthenticationFilter JWT 认证过滤器实例
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器 Bean —— 用于用户注册时对明文密码进行加密存储
     *
     * BCryptPasswordEncoder 的特点：
     * - 单向哈希：无法从密文反推出明文
     * - 自动加盐：每次加密相同的密码会得到不同的结果（随机盐值嵌入在哈希结果中）
     * - 可调强度：构造参数 strength 默认为 10（迭代 2^10 次），越强越安全但也越慢
     * - 长度固定：输出字符串始终是 60 个字符，格式为 $2a$10$...
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链（核心配置方法）—— 定义所有安全规则
     *
     * 请求处理流程：
     *   HTTP 请求 → Spring Security 过滤器链 → JwtAuthenticationFilter（解析 Token 设身份）
     *   → 权限规则匹配（本方法配置）→ 匹配成功 → Controller
     *                     ↓ 匹配失败
     *                  返回 401/403
     *
     * @param http HttpSecurity 对象，由 Spring Security 自动注入
     * @return 构建好的 SecurityFilterChain
     * @throws Exception 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ── 1. 禁用 CSRF（跨站请求伪造保护）──
                // 传统表单登录容易受到 CSRF 攻击，所以 Spring Security 默认开启 CSRF 保护。
                // 但 RESTful API 使用 JWT Token 认证，Token 一般放在 Header 中，
                // 攻击者无法通过第三方网站构造 Header，因此不需要 CSRF 保护。
                .csrf(csrf -> csrf.disable())

                // ── 2. 无状态会话配置 ──
                // 设置 Session 创建策略为 STATELESS（无状态）
                // 这意味着 Spring Security 不会创建 HttpSession，也不会从 HttpSession 获取认证信息
                // 每一个请求都必须携带 JWT Token 进行独立认证
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── 3. URL 权限规则配置 ──
                // 规则匹配顺序：从上到下，先匹配到的优先
                // 如果多个规则冲突，前面的规则生效
                .authorizeHttpRequests(auth -> auth
                        // ===== 3.1 公开接口（无需登录，游客可访问） =====
                        .requestMatchers("/api/auth/**").permitAll()                      // 登录（/api/auth/login）、注册（/api/auth/register）
                        .requestMatchers("/api/music/file/**").permitAll()                // 音乐文件（MP3 等），前端页面直接引用
                        .requestMatchers("/api/music/cover/**").permitAll()               // 封面图片，前端页面直接引用
                        .requestMatchers("/api/music/list").permitAll()                   // 音乐列表，任何人都可以浏览
                        .requestMatchers(HttpMethod.GET, "/api/music/*").permitAll()      // 音乐详情（GET 请求）
                        .requestMatchers(HttpMethod.GET, "/api/music/*/stream").permitAll() // 在线播放（GET 请求）
                        .requestMatchers(HttpMethod.GET, "/api/ranking/**").permitAll()   // 排行榜（GET 请求）
                        .requestMatchers(HttpMethod.GET, "/api/external/**").permitAll()  // 外部音乐搜索、歌单、歌词查询（GET 请求）

                        // ===== 3.2 管理员专用接口 =====
                        // hasRole("ADMIN") 会自动添加 ROLE_ 前缀，检查用户是否有 ROLE_ADMIN 权限
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== 3.3 其他所有 /api/ 开头的接口需要登录认证 =====
                        // 这包括：音乐上传、评论 CRUD、点赞、下载记录等
                        .requestMatchers("/api/**").authenticated()

                        // ===== 3.4 静态资源（前端页面、CSS、JS 等）允许所有 =====
                        // 即根路径 /index.html、/css/ 等不需要登录
                        .anyRequest().permitAll()
                )

                // ── 4. 插入自定义 JWT 过滤器 ──
                // addFilterBefore() 方法将自定义过滤器添加到指定过滤器之前执行
                // 这里放到 UsernamePasswordAuthenticationFilter 之前，确保在 Spring 内置的表单登录过滤器
                // 执行之前，我们的 JWT 过滤器已经完成了 Token 解析和认证信息设置
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        // 构建并返回 SecurityFilterChain 对象
        return http.build();
    }
}