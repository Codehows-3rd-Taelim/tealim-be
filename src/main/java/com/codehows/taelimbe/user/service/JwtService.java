package com.codehows.taelimbe.user.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.security.Key;

@Service
public class JwtService {

    // 서버와 클라이언트가 주고 받는 토큰 ==> HTTP Header 내 Authorization 헤더값에 저장
    // 예) Authorization Bearer <토큰값>
    static final String PREFIX = "Bearer ";
    static final Key SIGNING_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // loginId(ID)를 받아서 JWT 생성
    public String generateToken(String loginId)
    {
        return Jwts.builder()
                .setSubject(loginId)
                .signWith(SIGNING_KEY)
                .compact();
    }

    // JWT를 받아서 id(ID)를 반환
    public String parseToken(HttpServletRequest request)
    {
        // 요청 헤더에서 Authorization 헤더값을 가져옴
        // 예) header = Bearer <토큰값>
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX))
        {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build();

            String id = parser.parseClaimsJws(header.replace(PREFIX, ""))
                    .getBody()
                    .getSubject();
            if (id != null)
            {
                return id;
            }
        }
        return null;
    }

}
