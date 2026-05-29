package org.example.musicdemo.config;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 —— JSON Web Token 的生成、解析、验证
 *
 * JWT 的结构：Header.Payload.Signature（三部分，用点号分隔，Base64Url 编码）
 * - Header：   声明算法（HS256）和类型（JWT）
 * - Payload：  存储用户数据（user_id, username, role）+ 签发时间 + 过期时间
 * - Signature：对前两部分签名，防止篡改（只有拥有密钥的服务端才能验证和生成）
 *
 * 本系统使用 jjwt 0.12.6 库（io.jsonwebtoken），
 * 密钥从 application.yml 读取，加密方式为 HMAC-SHA 系列。
 *
 * ─── 密钥与过期时间 ───
 * - secret 由 application.yml 中的 jwt.secret 配置（开发阶段写在配置文件，生产环境应从环境变量读取）
 * - expiration 由 jwt.expiration 配置，单位毫秒，注意开发环境可能设为 20000（20秒）用于调试
 * - 密钥字符串通过 Keys.hmacShaKeyFor() 方法生成 SecretKey 对象
 */
@Component  // 交给 Spring 管理，配合 @Value 注入配置到字段
public class JwtUtils {

    /**
     * HMAC-SHA 密钥对象 —— 用于签名和验签
     * 由密钥字符串通过 Keys.hmacShaKeyFor() 生成，确保密钥强度足够
     * 注意：密钥字符串太短会抛出异常，建议至少 256 位（32 个字符）
     */
    private final SecretKey key;

    /**
     * Token 过期时间，单位毫秒
     * 从 application.yml 的 jwt.expiration 注入
     * 生产环境建议设为 604800000（7天）或 86400000（1天）
     */
    private final long expiration;

    /**
     * 构造器注入配置（Spring 官方推荐的方式）
     *
     * @param secret     JWT 签名的密钥字符串，从 {@code jwt.secret} 读取
     * @param expiration Token 过期时间（毫秒），从 {@code jwt.expiration} 读取
     */
    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration) {
        // 将 UTF-8 编码的密钥字符串转换为 HMAC-SHA 算法的 SecretKey 对象
        // Keys.hmacShaKeyFor() 会根据传入字节数组的长度选择合适的 HMAC 算法（HS256/HS384/HS512）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token —— 用户登录成功后调用，返回给前端
     *
     * @param userId   用户 ID（整型），存储在 Token 的 subject（主题）字段中
     * @param username 用户名，存储在自定义 claim "username" 中
     * @param role     用户角色（"user" 或 "admin"），存储在自定义 claim "role" 中
     * @return 生成的 JWT 字符串，形如 "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.xxx"
     */
    public String generateToken(Integer userId, String username, String role) {
        Date now = new Date();
        // 过期时间 = 当前时间 + 配置的过期毫秒数
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))       // sub（Subject）：主题，这里存用户 ID 的字符串形式
                .claim("username", username)            // 自定义声明：用户名，用于页面展示和日志
                .claim("role", role)                    // 自定义声明：角色，用于 Spring Security 权限判断
                .issuedAt(now)                          // iat（Issued At）：签发时间，用于计算 Token 年龄
                .expiration(expiryDate)                 // exp（Expiration）：过期时间，超过此时间的 Token 将被拒绝
                .signWith(key)                          // 使用 HMAC 密钥签名，确保 Token 内容未被篡改
                .compact();                             // 将上述所有信息压缩成最终的 JWT 字符串
    }

    /**
     * 从 Token 中解析出 Claims（载荷体）
     * Claims 包含 subject、自定义字段、签发时间、过期时间等全部信息
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims 对象
     * @throws JwtException           如果 Token 签名无效、过期、格式错误等
     * @throws IllegalArgumentException 如果传入的 token 为 null 或空字符串
     */
    public Claims parseToken(String token) {
        return Jwts.parser()               // 创建解析器构建器
                .verifyWith(key)           // 设置验证密钥（必须与签名时使用的密钥一致）
                .build()                   // 构建解析器
                .parseSignedClaims(token)  // 解析并验证签名（如果签名无效或 Token 过期，此处抛出异常）
                .getPayload();             // 获取 JWS 的 Payload 部分，即 Claims 对象
    }

    /**
     * 验证 Token 是否有效（签名正确 + 未过期）
     *
     * @param token JWT 字符串
     * @return true 表示 Token 有效，false 表示无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析 —— 如果解析成功且未抛出异常，说明 Token 有效
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException：签名不匹配、Token 已过期、格式错误等
            // IllegalArgumentException：token 为 null
            return false;
        }
    }

    /**
     * 从 Token 中提取用户 ID（Subject 字段中存储的）
     * 用于在 Controller/Service 中快速获取当前操作用户
     *
     * @param token JWT 字符串
     * @return 用户 ID（Integer 类型）
     */
    public Integer getUserId(String token) {
        // Subject 存储的是字符串形式的 userId，需要转换为 Integer
        String subject = parseToken(token).getSubject();
        return Integer.parseInt(subject);
    }
}