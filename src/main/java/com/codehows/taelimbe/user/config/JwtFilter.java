package com.codehows.taelimbe.user.config;

import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.security.UserPrincipal;
import com.codehows.taelimbe.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter
{
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // SSE 알림 연결은 JWT 필터 제외
        String uri = request.getRequestURI();

        if (uri.startsWith("/events/notifications")) {
            filterChain.doFilter(request, response);
            return;
        }


        // OPTIONS 요청(Preflight)은 JWT 검증 없이 바로 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 없으면 query parameter에서 토큰 확인 (SSE용)
        if (authHeader == null) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isEmpty()) {
                authHeader = "Bearer " + tokenParam;
                log.debug("JWT token found in query parameter for SSE connection");
            }
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String username = jwtService.parseToken(authHeader);
                Long userId = jwtService.extractUserId(authHeader);

                if (username != null && userId != null) {
                    UserPrincipal principal = new UserPrincipal(userId, username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    Collections.emptyList()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication successful for user: {} (userId: {})", username, userId);
                } else {
                    log.warn("Failed to parse JWT token - username or userId is null");
                }
            } catch (Exception e) {
                log.error("JWT token validation failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터 체인 실행
        filterChain.doFilter(request, response);
    }
}