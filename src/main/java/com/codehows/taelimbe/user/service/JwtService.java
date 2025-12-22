package com.codehows.taelimbe.user.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // 서버와 클라이언트가 주고 받는 토큰 ==> HTTP Header 내 Authorization 헤더값에 저장
    // 예) Authorization Bearer <토큰값>
    private static final String PREFIX = "Bearer ";

    private final long expirationTime;
    private final SecretKey signingKey;

    // 생성자를 통해 고정 키 주입
    public JwtService(
            @Value("${jwt.secret-key}") String secretKeyString,
            @Value("${jwt.expiration}") long expirationTime) {
        // 고정된 시크릿 키를 SecretKey 객체로 변환
        this.signingKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    // loginId(ID)를 받아서 JWT 생성
    public String generateToken(String username, Long userId, boolean isAdmin, Long storeId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("isAdmin", isAdmin)
                .claim("storeId", storeId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // ← 수정
                .signWith(signingKey, SignatureAlgorithm.HS256) // ← 수정
                .compact();
    }

    public Long extractUserId(String token) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build();

            return parser.parseClaimsJws(token)
                    .getBody()
                    .get("userId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean extractIsAdmin(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("isAdmin", Boolean.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractStoreId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("storeId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    // JWT를 받아서 id(ID)를 반환
    public String parseToken(String token) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build();

            return parser.parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

}