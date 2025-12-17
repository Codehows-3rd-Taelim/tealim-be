package com.codehows.taelimbe.notification.controller;

import com.codehows.taelimbe.notification.entity.Notification;
import com.codehows.taelimbe.notification.repository.NotificationRepository;
import com.codehows.taelimbe.notification.service.NotificationService;
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
    private final NotificationRepository notificationRepository;

    //SSE 연결 (알림용)
    @GetMapping("/events/notifications")
    public SseEmitter connect(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return notificationService.connect(user.userId());
    }

    //아직 토스트 안 뜬 알림 조회
    @GetMapping("/notifications/undelivered")
    public List<Notification> getUndelivered(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return notificationRepository
                .findByUserIdAndDeliveredAtIsNull(user.userId());
    }

    // 토스트 노출 완료 처리
    @PostMapping("/notifications/{id}/delivered")
    public void markDelivered(@PathVariable Long id) {
        notificationService.markDelivered(id);
    }
}
