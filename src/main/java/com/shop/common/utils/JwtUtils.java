package com.shop.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${shop.jwt.secret}")
    private String secret;

    @Value("${shop.jwt.expire:604800}")
    private long expire;

    @Value("${shop.jwt.admin-expire:28800}")
    private long adminExpire;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成小程序用户 Token
     */
    public String generateToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "user");
        return buildToken(claims, expire * 1000L);
    }

    /**
     * 生成管理员 Token
     */
    public String generateAdminToken(Long adminId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("type", "admin");
        return buildToken(claims, adminExpire * 1000L);
    }

    private String buildToken(Map<String, Object> claims, long expireMs) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 Token 获取 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取管理员ID
     */
    public Long getAdminId(String token) {
        Claims claims = parseToken(token);
        return claims.get("adminId", Long.class);
    }

    /**
     * 判断是否管理员 Token
     */
    public boolean isAdminToken(String token) {
        Claims claims = parseToken(token);
        return "admin".equals(claims.get("type"));
    }

    /**
     * 校验 Token 有效性
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Token 校验失败: {}", e.getMessage());
            return false;
        }
    }
}
