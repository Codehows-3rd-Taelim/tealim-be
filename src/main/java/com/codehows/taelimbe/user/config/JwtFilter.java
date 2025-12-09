package com.codehows.taelimbe.user.config;

import com.codehows.taelimbe.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter
{
    private final JwtService jwtService;
    private final Servlet servlet;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // OPTIONS 요청(Preflight)은 JWT 검증 없이 바로 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 필터 ==> 요청, 응답을 중간에서 가로챈 다음 ==> 필요한 동작을 수행
        // 1. 요청 헤더 (Authorization)에서 JWT 토큰을 꺼냄
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        System.out.println("📌 [JWT FILTER] URI = " + request.getRequestURI() + " | Authorization = " + jwtToken);
        if (jwtToken != null)
        {
            // 2. 꺼낸 토큰에서 유저 정보 추출
            String id = jwtService.parseToken(request);

            // 2) userId(claim) 추출
            Long userId = jwtService.extractUserId(jwtToken);

            // 3. 추출된 유저 정보로 Authentication 을 만들어서 SecurityContext에 set
            if(id != null)
            {
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(id, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // Controller 에서 userId 사용할 수 있도록 저장
            request.setAttribute("userId", userId);

        }
        // 마지막에 다음 필터를 호출
        filterChain.doFilter(request, response);
    }
}