package com.codehows.taelimbe.notification.controller;

import com.codehows.taelimbe.notification.dto.NotificationDTO;
import com.codehows.taelimbe.notification.service.NotificationService;
import com.codehows.taelimbe.notification.service.NotificationSseService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;

    // SSE 연결
    @GetMapping("/events/notifications")
    public SseEmitter connect(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return notificationSseService.connect(user.userId());
    }

    // 토스트 대상
    @GetMapping("/notifications/undelivered")
    public List<NotificationDTO> undelivered(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return notificationService.getUndelivered(user.userId());
    }

    // 알림 목록
    @GetMapping("/notifications")
    public List<NotificationDTO> list(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return notificationService.getAll(user.userId());
    }

    // 토스트 노출 완료
    @PostMapping("/notifications/{id}/delivered")
    public void delivered(@PathVariable Long id) {
        notificationService.markDelivered(id);
    }

    // 읽음 처리
    @PostMapping("/notifications/{id}/read")
    public void read(@PathVariable Long id) {
        notificationService.markRead(id);
    }
}
