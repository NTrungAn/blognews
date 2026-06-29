package com.blog.blogsystem.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // Chuỗi bí mật dùng để ký token (Phải dài tối thiểu 256-bit / 32 ký tự)
    // Trong thực tế, nên đưa chuỗi này vào application.properties
    private final String JWT_SECRET = "ChonMotChuoiBiMatThatDaiVaAnToanChoHeThongBlog2026";

    // Thời gian sống của Access Token: 15 phút
    private final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;
    
    // Thời gian sống của Refresh Token: 7 ngày
    private final long REFRESH_TOKEN_EXPIRATION = 7L * 24 * 60 * 60 * 1000;

    private final Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    // 1. Tạo Access Token từ Username
    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Tạo Refresh Token từ Username
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Lấy Username từ chuỗi JWT
    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // 3. Kiểm tra tính hợp lệ của Token
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Em có thể log lỗi cụ thể ở đây (Expired, Malformed, Unsupported...)
        }
        return false;
    }
}
