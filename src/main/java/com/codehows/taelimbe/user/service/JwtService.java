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
    public String generateToken(String loginId) {
        return Jwts.builder()
                .setSubject(loginId)
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT를 받아서 id(ID)를 반환
    public String parseToken(HttpServletRequest request) {
        // 요청 헤더에서 Authorization 헤더값을 가져옴
        // 예) header = Bearer <토큰값>
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            try {
                JwtParser parser = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build();

                String id = parser.parseClaimsJws(header.replace(PREFIX, ""))
                        .getBody()
                        .getSubject();

                return id;
            } catch (Exception e) {
                // 토큰 파싱 실패 시 null 반환
                return null;
            }
        }
        return null;
    }
}