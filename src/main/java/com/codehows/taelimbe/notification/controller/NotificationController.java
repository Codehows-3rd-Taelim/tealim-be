package com.codehows.taelimbe.notification.controller;

import com.codehows.taelimbe.notification.entity.Notification;
import com.codehows.taelimbe.notification.repository.NotificationRepository;
import com.codehows.taelimbe.notification.service.NotificationService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    /* ===== SSE 연결 ===== */
    @GetMapping("/events/notifications")
    public SseEmitter connect(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        log.info("🔥 SSE CONNECT userId={}", user.userId());
        return notificationService.connect(user.userId());
    }

    /* ===== 안 읽은 알림 조회 ===== */
    @GetMapping("/notifications/unread")
    public List<Notification> unread(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.userId());
    }

    /* ===== 읽음 처리 ===== */
    @PostMapping("/notifications/{notificationId}/read")
    public void read(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow();

        if (!n.getUserId().equals(user.userId())) return;

        n.markAsRead();
        notificationRepository.save(n);
    }
}
