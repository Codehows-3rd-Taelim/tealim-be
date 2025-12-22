package com.codehows.taelimbe.user.config;

import com.codehows.taelimbe.user.security.UserPrincipal;
import com.codehows.taelimbe.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        // OPTIONS 요청(Preflight)은 JWT 검증 없이 바로 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                // Bearer 제거
                String token = authHeader.substring(7);

                // JwtService에는 순수 token만 넘김
                String username = jwtService.parseToken(token);
                Long userId = jwtService.extractUserId(token);
                Boolean isAdmin = jwtService.extractIsAdmin(token);
                Long storeId = jwtService.extractStoreId(token);

                if (username != null && userId != null) {
                    UserPrincipal principal = new UserPrincipal(
                            userId,
                            username,
                            isAdmin != null && isAdmin,
                            storeId
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    Collections.emptyList()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug(
                            "Authentication successful for user: {} (userId: {})",
                            username,
                            userId
                    );
                } else {
                    if (request.getRequestURI().startsWith("/events/notifications")) {
                        log.debug(
                                "SSE JWT parse skipped, uri={}",
                                request.getRequestURI()
                        );
                    } else {
                        log.warn(
                                "Failed to parse JWT token - username or userId is null, uri={}",
                                request.getRequestURI()
                        );
                    }

                }
            } catch (Exception e) {
                log.warn(
                        "JWT token validation failed, uri={}, msg={}",
                        request.getRequestURI(),
                        e.getMessage()
                );
                SecurityContextHolder.clearContext();
            }
        }
        // 다음 필터 체인 실행
        filterChain.doFilter(request, response);
    }



}