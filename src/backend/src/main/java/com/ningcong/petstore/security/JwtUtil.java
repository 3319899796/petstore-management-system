package com.ningcong.petstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与校验令牌。
 *
 * <p>令牌是无状态的：服务端不存会话，只验签名 + 过期时间。
 * 载荷放 uid（用户主键）、rid（角色主键），不放密码和权限明细（权限每次请求从库里取，改角色即时生效）。</p>
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        // HS256 要求密钥长度 ≥ 32 字节，yml 里已保证
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    /** 签发令牌。 */
    public String createToken(Integer userId, String username, Integer roleId) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000L);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .claim("rid", roleId)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验并解析令牌。
     *
     * @throws io.jsonwebtoken.JwtException 签名不对、格式不对或已过期
     */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
