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
 * JWT 工具类
 * 负责：生成 Token、解析 Token、验证 Token
 */
@Component  // 交给 Spring 管理，可以用 @Value 注入配置
public class JwtUtils {

    private final SecretKey key;
    private final long expiration;

    /**
     * 构造器注入配置
     * @Value 从 application.yml 中读取 jwt.secret 和 jwt.expiration
     */
    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration) {
        // 用密钥字符串生成 HMAC-SHA 密钥
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色（user / admin）
     */
    public String generateToken(Integer userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))       // sub：主题，存用户 ID
                .claim("username", username)            // 自定义 claim：用户名
                .claim("role", role)                    // 自定义 claim：角色
                .issuedAt(now)                          // 签发时间
                .expiration(expiryDate)                 // 过期时间
                .signWith(key)                          // 签名
                .compact();                             // 生成字符串
    }

    /**
     * 从 Token 中解析出 Claims（存储的信息）
     * @throws JwtException 如果 Token 无效或过期
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 Token 获取用户 ID
     */
    public Integer getUserId(String token) {
        String subject = parseToken(token).getSubject();
        return Integer.parseInt(subject);
    }
}