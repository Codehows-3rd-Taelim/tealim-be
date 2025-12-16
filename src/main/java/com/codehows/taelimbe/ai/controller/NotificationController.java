package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.service.NotificationService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import com.codehows.taelimbe.user.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    @GetMapping(value = "/events/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Long userId = jwtService.extractUserId(authorization);

        if (userId == null) {
            throw new RuntimeException("Invalid JWT token");
        }

        log.info("Notification SSE connected. userId={}", userId);
        return notificationService.connect(userId);
    }
}
